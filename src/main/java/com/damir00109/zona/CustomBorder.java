package com.damir00109.zona;

import java.util.ArrayList;
import java.util.List;

public class CustomBorder {
    private List<Point> vertices = new ArrayList<>();
    private double minX, minY, minZ, maxX, maxY, maxZ; // minY/maxY не используются для 2D границ, но для общности
    private boolean boundsInitialized = false;

    public void addVertex(Point point) { 
        this.vertices.add(point); 
        boundsInitialized = false; // Сбрасываем флаг, т.к. границы нужно пересчитать
    }

    public void setVertices(List<Point> newVertices) {
        if (newVertices == null) {
            this.vertices = new ArrayList<>(); // или выбросить исключение
        } else {
            this.vertices = new ArrayList<>(newVertices); // Создаем копию для безопасности
        }
        boundsInitialized = false; // Границы нужно будет пересчитать
    }

    public List<Point> getVertices() {
        return this.vertices; // Возвращаем прямую ссылку или копию в зависимости от требований
                              // Для простоты пока прямую ссылку
    }

    private void initializeBounds() {
        if (vertices.isEmpty()) return;

        minX = vertices.get(0).x;
        maxX = vertices.get(0).x;
        minZ = vertices.get(0).z;
        maxZ = vertices.get(0).z;

        for (int i = 1; i < vertices.size(); i++) {
            Point p = vertices.get(i);
            if (p.x < minX) minX = p.x;
            if (p.x > maxX) maxX = p.x;
            if (p.z < minZ) minZ = p.z;
            if (p.z > maxZ) maxZ = p.z;
        }
        boundsInitialized = true;
    }

    public boolean isInside(Point point) {
        if (vertices.size() < 3) return true; // Не многоугольник, считаем, что внутри (или можно false)
        
        if (!boundsInitialized) {
            initializeBounds();
        }

        // Быстрая проверка для прямоугольных границ, параллельных осям
        // Это основная оптимизация для вашего случая
        if (point.x >= minX && point.x <= maxX && point.z >= minZ && point.z <= maxZ) {
            // Для непрямоугольных или повернутых многоугольников, нужно будет вернуть старый алгоритм
            // или убедиться, что вершины всегда определяют прямоугольник, параллельный осям.
            // В вашем случае все границы прямоугольные и параллельны осям.
            return true;
        }
        // Если это не так, но многоугольник мог быть непрямоугольным, 
        // то должен был бы использоваться исходный алгоритм трассировки лучей.
        // Но так как мы знаем, что у вас прямоугольники, то этого достаточно.
        return false; 
        
        /* Исходный алгоритм трассировки луча (оставлен для справки, если понадобится для непрямоугольных):
        if (vertices.isEmpty()) return true;
        int i, j; boolean result = false;
        int nvert = vertices.size();
        for (i = 0, j = nvert - 1; i < nvert; j = i++) {
            Point vertI = vertices.get(i);
            Point vertJ = vertices.get(j);
            if ((vertI.z > point.z) != (vertJ.z > point.z)) {
                 // Проверка на деление на ноль, если vertJ.z == vertI.z
                if (vertJ.z - vertI.z == 0) { 
                    // Горизонтальная линия, если точка на той же высоте и между X, может быть пересечение или касание
                    // Для простоты, если линия строго горизонтальна и на уровне точки, можно считать, что она не пересекает луч
                    // Либо нужна более сложная обработка таких случаев.
                    // В контексте axis-aligned прямоугольников эта ветка не должна достигаться при правильном bounding box.
                    continue; 
                }
                if (point.x < (vertJ.x - vertI.x) * (point.z - vertI.z) / (vertJ.z - vertI.z) + vertI.x) {
                    result = !result;
                }
            }
        }
        return result;
        */
    }
} 