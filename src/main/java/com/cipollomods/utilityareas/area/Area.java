package com.cipollomods.utilityareas.area;

/**
 * Clase base abstracta para todos los tipos de área.
 * Un área define una zona geométrica en el mundo con un comportamiento asociado.
 */
public abstract class Area {

    protected final String id;
    protected final AreaType type;
    protected final AreaShape shape;
    protected boolean active;

    // Campos para área circular
    protected double centerX;
    protected double centerZ;
    protected double radius;

    // Campos para área rectangular
    protected double x1, z1, x2, z2;

    protected Area(String id, AreaType type, AreaShape shape) {
        this.id = id;
        this.type = type;
        this.shape = shape;
        this.active = true;
    }

    /**
     * Comprueba si las coordenadas dadas están dentro del área.
     * Utiliza distancia euclidiana para círculos y AABB para rectángulos.
     */
    public boolean contains(double x, double z) {
        if (shape == AreaShape.CIRCLE) {
            double dx = x - centerX;
            double dz = z - centerZ;
            return (dx * dx + dz * dz) <= (radius * radius);
        } else {
            return x >= Math.min(x1, x2) && x <= Math.max(x1, x2)
                    && z >= Math.min(z1, z2) && z <= Math.max(z1, z2);
        }
    }


    public String getId() { return id; }
    public AreaType getType() { return type; }
    public AreaShape getShape() { return shape; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public double getCenterX() { return centerX; }
    public double getCenterZ() { return centerZ; }
    public double getRadius() { return radius; }

    // Metodo que cada tipo de área implementará con su lógica propia
    public abstract void onPlayerEnter(net.minecraft.world.entity.player.Player player);
    public abstract void onPlayerExit(net.minecraft.world.entity.player.Player player);
    public abstract void onTick(net.minecraft.server.level.ServerLevel level);

    // Constructor estático para área circular
    public static <T extends Area> T circle(T area, double centerX, double centerZ, double radius) {
        area.centerX = centerX;
        area.centerZ = centerZ;
        area.radius = radius;
        return area;
    }

    // Constructor estático para área rectangular
    public static <T extends Area> T rect(T area, double x1, double z1, double x2, double z2) {
        area.x1 = x1;
        area.z1 = z1;
        area.x2 = x2;
        area.z2 = z2;
        return area;
    }

    public double getX1() { return x1; }
    public double getZ1() { return z1; }
    public double getX2() { return x2; }
    public double getZ2() { return z2; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Área '").append(id).append("' [").append(type).append("] ")
                .append(active ? "(activa)" : "(inactiva)").append("\n");
        if (shape == AreaShape.CIRCLE) {
            sb.append("  Forma: círculo, centro=(").append(centerX).append(", ").append(centerZ)
                    .append("), radio=").append(radius).append("\n");
        } else {
            sb.append("  Forma: rectángulo, de (").append(x1).append(", ").append(z1)
                    .append(") a (").append(x2).append(", ").append(z2).append(")\n");
        }
        sb.append(describeSpecific());
        return sb.toString();
    }

    /**
     * Hook para que cada subclase añada sus propios campos a la salida de
     * /ua info. Por defecto no añade nada (p. ej. SafeArea no lo necesita).
     */
    protected String describeSpecific() {
        return "";
    }
}