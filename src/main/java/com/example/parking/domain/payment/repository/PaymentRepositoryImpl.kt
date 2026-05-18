
package com.example.parking.domain.payment.repository

import com.example.parking.domain.payment.entity.Payment
import com.example.parking.domain.payment.entity.PaymentStatus
import com.example.parking.domain.payment.entity.QPayment
import com.example.parking.domain.reservation.entity.QReservation
import com.example.parking.domain.user.entity.QUser
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
class PaymentRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : PaymentRepositoryCustom {

    override fun findAllByStatus(status: PaymentStatus): List<Payment> {
        val payment = QPayment.payment
        val reservation = QReservation.reservation
        val user = QUser.user

        return queryFactory
            .selectFrom(payment)
            .join(payment.reservation, reservation).fetchJoin()
            .join(reservation.user, user).fetchJoin()
            .where(payment.status.eq(status))
            .fetch()
    }
}