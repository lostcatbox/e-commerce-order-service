package kr.hhplus.be.server.core.point.domain

/**
 * 사용자 포인트 도메인 모델
 */
class UserPoint(
    val userId: Long,
    private var balance: Long,
    private var lastUpdatedAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val MIN_BALANCE = 0L // 최소 잔액
        const val MAX_BALANCE = 2_000_000L // 최대 잔액
        const val MIN_CHARGE_AMOUNT = 1L // 최소 충전 금액
        const val MAX_CHARGE_AMOUNT = 2_000_000L // 최대 충전 금액
        const val MIN_USE_AMOUNT = 1L // 최소 사용 금액
        const val MAX_USE_AMOUNT = 2_000_000L // 최대 사용 금액
    }

    init {
        require(balance >= MIN_BALANCE) { "잔액은 $MIN_BALANCE 이상이어야 합니다. 현재 잔액: $balance" }
        require(balance <= MAX_BALANCE) { "잔액은 $MAX_BALANCE 이하여야 합니다. 현재 잔액: $balance" }
    }

    /**
     * 현재 잔액 조회
     */
    fun getBalance(): Long = balance

    /**
     * 마지막 업데이트 시간 조회
     */
    fun getLastUpdatedAt(): Long = lastUpdatedAt

    /**
     * 포인트 충전
     * @param amount 충전할 금액
     */
    fun charge(amount: Long) {
        // param 검증
        validateChargeAmount(amount)
        val newBalance = balance + amount
        // 잔액 검증
        validateBalance(newBalance)

        this.balance = newBalance
        this.lastUpdatedAt = System.currentTimeMillis()
    }

    /**
     * 포인트 사용
     * @param amount 사용할 금액
     */
    fun use(amount: Long) {
        // param 검증
        validateUseAmount(amount)
        val newBalance = balance - amount
        // 잔액 검증
        validateBalance(newBalance)

        this.balance = newBalance
        this.lastUpdatedAt = System.currentTimeMillis()
    }

    private fun validateChargeAmount(amount: Long) {
        require(amount >= MIN_CHARGE_AMOUNT) {
            "충전 금액은 $MIN_CHARGE_AMOUNT 이상이어야 합니다. 요청 금액: $amount"
        }
        require(amount <= MAX_CHARGE_AMOUNT) {
            "충전 금액은 $MAX_CHARGE_AMOUNT 이하여야 합니다. 요청 금액: $amount"
        }
    }

    private fun validateUseAmount(amount: Long) {
        require(amount >= MIN_USE_AMOUNT) {
            "사용 금액은 $MIN_USE_AMOUNT 이상이어야 합니다. 요청 금액: $amount"
        }
        require(amount <= MAX_USE_AMOUNT) {
            "사용 금액은 $MAX_USE_AMOUNT 이하여야 합니다. 요청 금액: $amount"
        }
    }

    private fun validateBalance(newBalance: Long) {
        require(newBalance >= MIN_BALANCE) {
            "잔액이 $MIN_BALANCE 미만이 될 수 없습니다. 계산된 잔액: $newBalance"
        }
        require(newBalance <= MAX_BALANCE) {
            "잔액이 $MAX_BALANCE 초과할 수 없습니다. 계산된 잔액: $newBalance"
        }
    }
}
