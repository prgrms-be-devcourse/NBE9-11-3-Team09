package com.example.parking.domain.reservation.repository

import com.example.parking.domain.parkingLot.entity.QParkingLot.Companion.parkingLot
import com.example.parking.domain.parkingspot.entity.QParkingSpot.Companion.parkingSpot
import com.example.parking.domain.parkingspot.entity.SpotStatus
import com.example.parking.domain.reservation.entity.QReservation.Companion.reservation
import com.example.parking.domain.reservation.entity.Reservation
import com.example.parking.domain.reservation.entity.ReservationStatus
import com.example.parking.domain.user.entity.QUser.Companion.user
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.PathBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.support.PageableExecutionUtils
import java.time.LocalDateTime

class ReservationRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : ReservationRepositoryCustom {

    // [CUS-04] 예약 상세 조회 - 유저 본인 예약만
    override fun findQByIdAndUserId(reservationId: Long, userId: Long): Reservation? {
        return queryFactory
            .selectFrom(reservation)
            .join(reservation.user, user).fetchJoin()
            .join(reservation.parkingLot, parkingLot).fetchJoin()
            .join(reservation.parkingSpot, parkingSpot).fetchJoin()
            .where(
                reservation.id.eq(reservationId),
                reservation.user.id.eq(userId)
            )
            .fetchOne()
    }

    // 예약 단건 조회 (주차 자리 + 주차장 포함)
    override fun findQByIdWithParkingSpot(id: Long): Reservation? {
        return queryFactory
            .selectFrom(reservation)
            .join(reservation.parkingSpot, parkingSpot).fetchJoin()
            .join(parkingSpot.parkingLot, parkingLot).fetchJoin()
            .where(reservation.id.eq(id))
            .fetchOne()
    }

    // [CUS-04] 사용자 예약 목록 조회 - status 동적 필터링
    override fun findQAllByUserIdWithDetails(userId: Long, status: ReservationStatus?): List<Reservation> {
        return queryFactory
            .selectFrom(reservation)
            .join(reservation.parkingLot, parkingLot).fetchJoin()
            .join(reservation.parkingSpot, parkingSpot).fetchJoin()
            .where(
                reservation.user.id.eq(userId),
                status?.let { reservation.status.eq(it) }
            )
            .fetch()
    }

    // [ADM] 관리자 - 특정 유저 예약 목록 페이징 조회
    override fun findQAllByUserIdWithDetailsPage(userId: Long, pageable: Pageable): Page<Reservation> {
        val orderSpecifiers = getOrderSpecifiers(pageable)

        val content = queryFactory
            .selectFrom(reservation)
            .join(reservation.user, user).fetchJoin()
            .join(reservation.parkingLot, parkingLot).fetchJoin()
            .join(reservation.parkingSpot, parkingSpot).fetchJoin()
            .where(reservation.user.id.eq(userId))
            .orderBy(*orderSpecifiers)
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        return PageableExecutionUtils.getPage(content, pageable) {
            queryFactory
                .select(reservation.count())
                .from(reservation)
                .where(reservation.user.id.eq(userId))
                .fetchOne() ?: 0L
        }
    }

    // [ADM] 관리자 - 전체 예약 목록 페이징 조회
    override fun findQAllWithDetailsPage(pageable: Pageable): Page<Reservation> {
        val orderSpecifiers = getOrderSpecifiers(pageable)

        val content = queryFactory
            .selectFrom(reservation)
            .join(reservation.user, user).fetchJoin()
            .join(reservation.parkingLot, parkingLot).fetchJoin()
            .join(reservation.parkingSpot, parkingSpot).fetchJoin()
            .orderBy(*orderSpecifiers)
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        return PageableExecutionUtils.getPage(content, pageable) {
            queryFactory
                .select(reservation.count())
                .from(reservation)
                .fetchOne() ?: 0L
        }
    }

    // 예약 시간 중복 체크 (예약 생성 시)
    override fun countQOverlappingReservations(
        spotId: Long,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Long {
        return queryFactory
            .select(reservation.count())
            .from(reservation)
            .where(
                reservation.parkingSpot.id.eq(spotId),
                reservation.status.ne(ReservationStatus.CANCELED),
                reservation.startTime.lt(endTime),
                reservation.endTime.gt(startTime)
            )
            .fetchOne() ?: 0L
    }

    // 예약 시간 중복 체크 (결제 시)
    override fun countQOverlapping(spotId: Long, start: LocalDateTime, end: LocalDateTime): Long {
        return queryFactory
            .select(reservation.count())
            .from(reservation)
            .where(
                reservation.parkingSpot.id.eq(spotId),
                reservation.status.`in`(ReservationStatus.PENDING, ReservationStatus.CONFIRMED),
                reservation.endTime.gt(start),
                reservation.startTime.lt(end)
            )
            .fetchOne() ?: 0L
    }

    // 자동 체크인 대상 조회
    override fun findQToAutoCheckIn(now: LocalDateTime): List<Reservation> {
        return queryFactory
            .selectFrom(reservation)
            .join(reservation.parkingSpot, parkingSpot).fetchJoin()
            .join(parkingSpot.parkingLot, parkingLot).fetchJoin()
            .where(
                reservation.status.eq(ReservationStatus.CONFIRMED),
                reservation.startTime.loe(now)
            )
            .fetch()
    }

    // 자동 체크아웃 대상 조회
    override fun findQToAutoCheckOut(now: LocalDateTime): List<Reservation> {
        return queryFactory
            .selectFrom(reservation)
            .join(reservation.parkingSpot, parkingSpot).fetchJoin()
            .join(parkingSpot.parkingLot, parkingLot).fetchJoin()
            .where(
                reservation.status.eq(ReservationStatus.COMPLETED),
                reservation.endTime.loe(now),
                reservation.parkingSpot.status.eq(SpotStatus.PARKED)
            )
            .fetch()
    }

    // 결제 미진입 선점 타임아웃 대상 조회
    override fun findQSelectionTimeout(limit: LocalDateTime): List<Reservation> {
        return queryFactory
            .selectFrom(reservation)
            .join(reservation.parkingSpot, parkingSpot).fetchJoin()
            .join(parkingSpot.parkingLot, parkingLot).fetchJoin()
            .where(
                reservation.status.eq(ReservationStatus.PENDING),
                reservation.paymentRequestedAt.isNull,
                reservation.createdAt.lt(limit)
            )
            .fetch()
    }

    // 동일 주차장 진행 중인 예약 존재 여부 확인
    override fun existsQByUserIdAndDateAndStatusIn(
        userId: Long,
        date: java.time.LocalDate,
        statuses: List<ReservationStatus>
    ): Boolean {
        return queryFactory
            .selectOne()
            .from(reservation)
            .where(
                reservation.user.id.eq(userId),
                reservation.status.`in`(statuses),
                reservation.startTime.between(
                    date.atStartOfDay(),
                    date.plusDays(1).atStartOfDay()
                )
            )
            .fetchFirst() != null
    }

    // pageable.sort → OrderSpecifier 변환 (기본: createdAt DESC)
    private fun getOrderSpecifiers(pageable: Pageable): Array<OrderSpecifier<*>> {
        if (!pageable.sort.isSorted) {
            return arrayOf(reservation.createdAt.desc())
        }

        val pathBuilder = PathBuilder(Reservation::class.java, "reservation")
        return pageable.sort.map { order ->
            val path = pathBuilder.getComparable(order.property, Comparable::class.java)
            if (order.direction == Sort.Direction.ASC) path.asc() else path.desc()
        }.toList().toTypedArray()
    }
}