package kr.hhplus.be.server.core.user.domain

import jakarta.persistence.*

/**
 * 사용자 도메인 모델
 */
@Entity
@Table(name = "user")
class User(
    @Id
    @Column(name = "user_id")
    val userId: Long,

    @Column(name = "name", nullable = false)
    val name: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Long = System.currentTimeMillis(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Long = System.currentTimeMillis(),
) {
    init {
        require(userId > 0) { "사용자 ID는 0보다 커야 합니다. 입력된 ID: $userId" }
        require(name.isNotBlank()) { "사용자 이름은 비어있을 수 없습니다." }
    }
}
