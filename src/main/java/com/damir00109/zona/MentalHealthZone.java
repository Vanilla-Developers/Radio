package com.damir00109.zona;

import java.util.Objects;

public class MentalHealthZone {
    public String name;
    public double x1, z1, x2, z2; // Координаты двух противоположных углов
    public double recoveryRatePerMinute; // Единиц сознания в минуту
    public String dimensionId; // Идентификатор измерения

    // Пустой конструктор для Gson
    public MentalHealthZone() {}

    public MentalHealthZone(String name, double x1, double z1, double x2, double z2, double recoveryRatePerMinute, String dimensionId) {
        this.name = name;
        // Гарантируем, что x1, z1 - это min координаты, а x2, z2 - max
        this.x1 = Math.min(x1, x2);
        this.z1 = Math.min(z1, z2);
        this.x2 = Math.max(x1, x2);
        this.z2 = Math.max(z1, z2);
        this.recoveryRatePerMinute = recoveryRatePerMinute;
        this.dimensionId = dimensionId;
    }

    public boolean isInside(double playerX, double playerZ, String playerDimensionId) {
        if (!Objects.equals(this.dimensionId, playerDimensionId)) {
            return false;
        }
        return playerX >= x1 && playerX <= x2 && playerZ >= z1 && playerZ <= z2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MentalHealthZone that = (MentalHealthZone) o;
        return Objects.equals(name, that.name) && Objects.equals(dimensionId, that.dimensionId); // Учитываем и имя, и измерение
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, dimensionId);
    }

    @Override
    public String toString() {
        return String.format("'%s' (Dim: %s, Coords: %.1f,%.1f to %.1f,%.1f) Rate: %.2f/min", name, dimensionId, x1, z1, x2, z2, recoveryRatePerMinute);
    }
} 