package com.damir00109.zona;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZonaConfig {
    public boolean zonaEnabled;
    public List<PointData> borderNormalPoints;
    public List<PointData> borderReducedPoints;
    public List<PointData> emergencyBorderPoints;

    public ZonaConfig() {
        // Default value
        this.zonaEnabled = true;

        // Default border definitions
        this.borderNormalPoints = new ArrayList<>(Arrays.asList(
                new PointData(-2000, -2000),
                new PointData(2000, -2000),
                new PointData(2000, 2000),
                new PointData(-2000, 2000)
        ));
        this.borderReducedPoints = new ArrayList<>(Arrays.asList(
                new PointData(-1950, -1950),
                new PointData(1950, -1950),
                new PointData(1950, 1950),
                new PointData(-1950, 1950)
        ));
        this.emergencyBorderPoints = new ArrayList<>(Arrays.asList(
                new PointData(-2100, -2100),
                new PointData(2100, -2100),
                new PointData(2100, 2100),
                new PointData(-2100, 2100)
        ));
    }

    // Внутренний класс для хранения данных Point, чтобы Gson мог их сериализовать/десериализовать
    // без проблем с final полями в оригинальном Point.
    // Если Point уже подходит для Gson (например, имеет пустой конструктор и сеттеры, или Gson настроен для работы с final),
    // то этот внутренний класс может не понадобиться.
    // Однако, для надежности и простоты сериализации, такой подход часто используется.
    public static class PointData {
        public double x;
        public double z;

        // Пустой конструктор для Gson
        public PointData() {}

        public PointData(double x, double z) {
            this.x = x;
            this.z = z;
        }
    }
} 