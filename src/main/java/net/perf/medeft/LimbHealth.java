package net.perf.medeft;

public class LimbHealth {
    private float headHealth;
    private float bodyHealth;
    private float armsHealth;
    private float legsHealth;

    public LimbHealth(float headHealth, float bodyHealth, float armsHealth, float legsHealth) {
        this.headHealth = headHealth;
        this.bodyHealth = bodyHealth;
        this.armsHealth = armsHealth;
        this.legsHealth = legsHealth;
    }

    public float getHeadHealth() {
        return headHealth;
    }

    public float getBodyHealth() {
        return bodyHealth;
    }

    public float getArmsHealth() {
        return armsHealth;
    }

    public float getLegsHealth() {
        return legsHealth;
    }

    public void setHeadHealth(float headHealth) {
        this.headHealth = headHealth;
    }

    public void setBodyHealth(float bodyHealth) {
        this.bodyHealth = bodyHealth;
    }

    public void setArmsHealth(float armsHealth) {
        this.armsHealth = armsHealth;
    }

    public void setLegsHealth(float legsHealth) {
        this.legsHealth = legsHealth;
    }
}
