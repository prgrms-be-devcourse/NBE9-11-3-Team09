package com.example.parking.domain.user.repository

import com.example.parking.domain.user.entity.QUser
import com.example.parking.domain.user.entity.User
import com.example.parking.domain.user.entity.UserRole
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

class UserRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : UserRepositoryCustom {

    override fun searchAdminUsers(keyword: String?, pageable: Pageable): Page<User> {
        val user = QUser.user

        val baseCondition = user.role.eq(UserRole.USER)
        val keywordCondition = keywordContains(keyword)
        val condition = keywordCondition?.let { baseCondition.and(it) } ?: baseCondition

        val content = queryFactory
            .selectFrom(user)
            .where(condition)
            .orderBy(user.createdTime.desc(), user.id.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val total = queryFactory
            .select(user.count())
            .from(user)
            .where(condition)
            .fetchOne() ?: 0L

        return PageImpl(content, pageable, total)
    }

    private fun keywordContains(keyword: String?): BooleanExpression? {
        val trimmedKeyword = keyword?.trim()?.takeIf { it.isNotBlank() }
            ?: return null

        val user = QUser.user

        return user.name.containsIgnoreCase(trimmedKeyword)
            .or(user.email.containsIgnoreCase(trimmedKeyword))
    }
}