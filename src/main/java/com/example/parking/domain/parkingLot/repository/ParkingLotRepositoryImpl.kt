package com.example.parking.domain.parkingLot.repository

import com.example.parking.domain.parkingLot.entity.ParkingLot
import com.example.parking.domain.parkingLot.entity.QParkingLot.Companion.parkingLot
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

class ParkingLotRepositoryImpl(
    private val queryFactory: JPAQueryFactory
): ParkingLotRepositoryCustom {

    override fun search(keyword: String?, pageable: Pageable): Page<ParkingLot> {
        val parkingLots = queryFactory
            .selectFrom(parkingLot)
            .where(keywordCondition(keyword))
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val total = queryFactory
            .select(parkingLot.count())
            .from(parkingLot)
            .where(keywordCondition(keyword))
            .fetchOne() ?: 0L

        return PageImpl(parkingLots, pageable, total)
    }

    private fun keywordCondition(keyword: String?): BooleanExpression? {

        if (keyword.isNullOrBlank()) {
            return null
        }

        return parkingLot.name.contains(keyword)
            .or(parkingLot.address.contains(keyword))
    }
}