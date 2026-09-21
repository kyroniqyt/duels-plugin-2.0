package xyz.yourserver.duels.bot;

public enum BotDifficulty {
    EASY(4.0, 1000L, 2.5);

    private final double attackDamage;
    private final long attackCooldownMillis;
    private final double attackRange;

    BotDifficulty(double attackDamage, long attackCooldownMillis, double attackRange) {
        this.attackDamage = attackDamage;
        this.attackCooldownMillis = attackCooldownMillis;
        this.attackRange = attackRange;
    }

    public double getAttackDamage() {
        return attackDamage;
    }

    public long getAttackCooldownMillis() {
        return attackCooldownMillis;
    }

    public double getAttackRange() {
        return attackRange;
    }
}
