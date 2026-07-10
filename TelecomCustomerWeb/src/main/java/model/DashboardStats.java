package model;

import java.math.BigDecimal;

/**
 * Aggregate statistics shown on the dashboard cards.
 */
public class DashboardStats {

    private long totalCustomers;
    private BigDecimal totalBalance;
    private BigDecimal averageBalance;
    private BigDecimal highestBalance;
    private BigDecimal lowestBalance;

    public DashboardStats() {
        this.totalBalance = BigDecimal.ZERO;
        this.averageBalance = BigDecimal.ZERO;
        this.highestBalance = BigDecimal.ZERO;
        this.lowestBalance = BigDecimal.ZERO;
    }

    public long getTotalCustomers() {
        return totalCustomers;
    }

    public void setTotalCustomers(long totalCustomers) {
        this.totalCustomers = totalCustomers;
    }

    public BigDecimal getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(BigDecimal totalBalance) {
        this.totalBalance = totalBalance;
    }

    public BigDecimal getAverageBalance() {
        return averageBalance;
    }

    public void setAverageBalance(BigDecimal averageBalance) {
        this.averageBalance = averageBalance;
    }

    public BigDecimal getHighestBalance() {
        return highestBalance;
    }

    public void setHighestBalance(BigDecimal highestBalance) {
        this.highestBalance = highestBalance;
    }

    public BigDecimal getLowestBalance() {
        return lowestBalance;
    }

    public void setLowestBalance(BigDecimal lowestBalance) {
        this.lowestBalance = lowestBalance;
    }

    public String toJson() {
        return "{"
                + "\"totalCustomers\":" + totalCustomers + ","
                + "\"totalBalance\":" + totalBalance.toPlainString() + ","
                + "\"averageBalance\":" + averageBalance.toPlainString() + ","
                + "\"highestBalance\":" + highestBalance.toPlainString() + ","
                + "\"lowestBalance\":" + lowestBalance.toPlainString()
                + "}";
    }
}
