package com.jutjubic.jutjubic_backend.service.map;

public class TileUtil {

    public static double tileXToLon(int x, int z) {
        double n = Math.pow(2.0, z);
        return x / n * 360.0 - 180.0;
    }

    public static double tileYToLat(int y, int z) {
        double n = Math.pow(2.0, z);
        double rad = Math.atan(Math.sinh(Math.PI * (1 - 2.0 * y / n)));
        return Math.toDegrees(rad);
    }

    public static Bounds tileToBounds(int z, int x, int y) {
        double minLng = tileXToLon(x, z);
        double maxLng = tileXToLon(x + 1, z);
        double maxLat = tileYToLat(y, z);
        double minLat = tileYToLat(y + 1, z);
        return new Bounds(minLat, maxLat, minLng, maxLng);
    }

    public static int lonToTileX(double lon, int z) {
        double n = Math.pow(2.0, z);
        return (int) Math.floor((lon + 180.0) / 360.0 * n);
    }

    public static int latToTileY(double lat, int z) {
        double n = Math.pow(2.0, z);
        double latRad = Math.toRadians(lat);
        return (int) Math.floor((1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * n);
    }

    public static TileCoord latLonToTile(double lat, double lon, int z) {
        return new TileCoord(lonToTileX(lon, z), latToTileY(lat, z));
    }

    public record Bounds(double minLat, double maxLat, double minLng, double maxLng) {}
    public record TileCoord(int x, int y) {}
}
