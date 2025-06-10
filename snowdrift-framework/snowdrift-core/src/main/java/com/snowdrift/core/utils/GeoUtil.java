package com.snowdrift.core.utils;

import com.snowdrift.core.GeoPoint;

import java.util.List;

/**
 * GeoUtil
 * WGS84 地球坐标系
 * GCJ02 火星坐标系
 * BD09 百度坐标系
 *
 * @author gaoye
 * @date 2025/06/09 19:44:52
 * @description 坐标系工具类
 * @since 1.0
 */
public class GeoUtil {

    private static final double X_PI = 3.14159265358979324 * 3000.0 / 180.0;

    /**
     * 国内坐标边界
     */
    private static final double MIN_LNG = 72.004;
    private static final double MAX_LNG = 137.8347;
    private static final double MIN_LAT = 0.8293;
    private static final double MAX_LAT = 55.8271;

    /**
     * 圆周率
     */
    private static final double PI = 3.14159265358979323846;

    /**
     * 长轴半径
     */
    private static final double A = 6378245.0;

    /**
     * 偏心率平方
     */
    private static final double EE = 0.00669342162296594323;

    /**
     * GCJ02 转 WGS84
     */
    public static GeoPoint gcj02ToWgs84(GeoPoint gcj02) {
        if (!isInChina(gcj02)) {
            return gcj02;
        }
        double dLng = transformLng(gcj02.getLongitude() - 105.0, gcj02.getLatitude() - 35.0);
        double dLat = transformLat(gcj02.getLongitude() - 105.0, gcj02.getLatitude() - 35.0);
        double radLat = gcj02.getLatitude() / 180.0 * PI;
        double magic = Math.sin(radLat);
        magic = 1 - EE * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLng = (dLng * 180.0) / (A / sqrtMagic * Math.cos(radLat) * PI);
        dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI);
        double mgLng = gcj02.getLongitude() + dLng;
        double mgLat = gcj02.getLatitude() + dLat;
        double wgLng = gcj02.getLongitude() * 2 - mgLng;
        double wgLat = gcj02.getLatitude() * 2 - mgLat;
        return new GeoPoint(wgLng, wgLat);
    }

    /**
     * GCJ02 转 BD09
     */
    public static GeoPoint gcj02ToBd09(GeoPoint gcj02) {
        double z = Math.sqrt(gcj02.getLongitude() * gcj02.getLongitude() + gcj02.getLatitude() * gcj02.getLatitude()) + 0.00002 * Math.sin(gcj02.getLatitude() * X_PI);
        double theta = Math.atan2(gcj02.getLatitude(), gcj02.getLongitude()) + 0.000003 * Math.cos(gcj02.getLongitude() * X_PI);
        double bdLng = z * Math.cos(theta) + 0.0065;
        double bdLat = z * Math.sin(theta) + 0.006;
        return new GeoPoint(bdLng, bdLat);
    }

    /**
     * BD09 转 GCJ02
     */
    public static GeoPoint bd09ToGcj02(GeoPoint bd09) {
        double x = bd09.getLongitude() - 0.0065, y = bd09.getLatitude() - 0.006;
        double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * X_PI);
        double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * X_PI);
        double gcjLng = z * Math.cos(theta);
        double gcjLat = z * Math.sin(theta);
        return new GeoPoint(gcjLng, gcjLat);
    }

    /**
     * BD09 转 WGS84
     */
    public static GeoPoint bd09ToWgs84(GeoPoint bd09) {
        return gcj02ToWgs84(bd09ToGcj02(bd09));
    }

    /**
     * GWS84 转 GCJ02
     */
    public static GeoPoint wgs84ToGcj02(GeoPoint wgs84) {
        // 判断是否在国内
        if (!isInChina(wgs84)) {
            // 国外坐标不处理
            return wgs84;
        }
        // 计算转换后的经纬度坐标
        double dLng = transformLng(wgs84.getLongitude() - 105.0, wgs84.getLatitude() - 35.0);
        double dLat = transformLat(wgs84.getLongitude() - 105.0, wgs84.getLatitude() - 35.0);
        double radLat = wgs84.getLatitude() / 180.0 * PI;
        double magic = Math.sin(radLat);
        magic = 1 - EE * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLng = (dLng * 180.0) / (A / sqrtMagic * Math.cos(radLat) * PI);
        dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI);
        double mgLng = wgs84.getLongitude() + dLng;
        double mgLat = wgs84.getLatitude() + dLat;
        return new GeoPoint(mgLng, mgLat);
    }

    /**
     * WGS84 转 BD09
     */
    public static GeoPoint wgs84ToBd09(GeoPoint wgs84) {
        return gcj02ToBd09(wgs84ToGcj02(wgs84));
    }

    /**
     * 经度转换
     */
    private static double transformLng(double lng, double lat) {
        double ret = 300.0 + lng + 2.0 * lat + 0.1 * lng * lng + 0.1 * lng * lat + 0.1 * Math.sqrt(Math.abs(lng));
        ret += (20.0 * Math.sin(6.0 * lng * PI) + 20.0 * Math.sin(2.0 * lng * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(lng * PI) + 40.0 * Math.sin(lng / 3.0 * PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(lng / 12.0 * PI) + 300.0 * Math.sin(lng / 30.0 * PI)) * 2.0 / 3.0;
        return ret;
    }
    
    /**
     * 纬度转换
     */
    private static double transformLat(double lng, double lat) {
        double ret = -100.0 + 2.0 * lng + 3.0 * lat + 0.2 * lat * lat + 0.1 * lng * lat + 0.2 * Math.sqrt(Math.abs(lng));
        ret += (20.0 * Math.sin(6.0 * lng * PI) + 20.0 * Math.sin(2.0 * lng * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(lat * PI) + 40.0 * Math.sin(lat / 3.0 * PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(lat / 12.0 * PI) + 320 * Math.sin(lat * PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    /**
     * 判断是否在国内
     */
    public static boolean isInChina(GeoPoint geoPoint) {
        return geoPoint.getLongitude() > MIN_LNG && geoPoint.getLongitude() < MAX_LNG && geoPoint.getLatitude() > MIN_LAT && geoPoint.getLatitude() < MAX_LAT;
    }

    /**
     * 计算两点间的距离
     * Haversine公式
     */
    public static double distance(GeoPoint p1, GeoPoint p2) {
        // 将角度转换为弧度
        double lat1Rad = Math.toRadians(p1.getLatitude());
        double lat2Rad = Math.toRadians(p2.getLatitude());
        double lng1Rad = Math.toRadians(p1.getLongitude());
        double lng2Rad = Math.toRadians(p2.getLongitude());

        // 计算经纬度差值
        double deltaLat = lat2Rad - lat1Rad;
        double deltaLgt = lng2Rad - lng1Rad;

        // 应用Haversine公式
        double a = Math.pow(Math.sin(deltaLat / 2), 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.pow(Math.sin(deltaLgt / 2), 2);
        double c = 2 * Math.asin(Math.sqrt(a));

        // 计算距离
        return A * c;
    }

    /**
     * 方向解析
     *
     * @param direct 方向 0~359
     * @param simple 是否简写
     */
    public static String getDirection(int direct, boolean simple) {
        String direction = "方向值有误";
        if (direct < 0 || direct > 359) {
            return direction;
        }
        int split = direct / 90;
        int angle = direct % 90;
        switch (split) {
            case 0:
                if (angle == 0) {
                    direction = "正北";
                } else {
                    if (simple) {
                        direction = "东北";
                    } else {
                        if (angle <= 45) {
                            direction = String.format("北偏东%s度", angle);
                        } else {
                            direction = String.format("东偏北%s度", (90 - angle));
                        }
                    }
                }
                break;
            case 1:
                if (angle == 0) {
                    direction = "正东";
                } else {
                    if (simple) {
                        direction = "东南";
                    } else {
                        if (angle <= 45) {
                            direction = String.format("东偏南%s度", angle);
                        } else {
                            direction = String.format("南偏东%s度", (90 - angle));
                        }
                    }
                }
                break;
            case 2:
                if (angle == 0) {
                    direction = "正南";
                } else {
                    if (simple) {
                        direction = "西南";
                    } else {
                        if (angle <= 45) {
                            direction = String.format("南偏西%s度", angle);
                        } else {
                            direction = String.format("西偏南%s度", (90 - angle));
                        }
                    }
                }
                break;
            case 3:
                if (angle == 0) {
                    direction = "正西";
                } else {
                    if (simple) {
                        direction = "西北";
                    } else {
                        if (angle <= 45) {
                            direction = String.format("西偏北%s度", angle);
                        } else {
                            direction = String.format("北偏西%s度", (90 - angle));
                        }
                    }
                }
                break;
            default:
                break;
        }
        return direction;
    }

    /**
     * 判断点是否在圆内
     * 实现思路：判断点到圆心的距离是否大于半径
     *
     * @param point  点
     * @param center 圆心
     * @param radius 半径
     * @return 是否在圆内
     */
    public static boolean isInCircle(GeoPoint point, GeoPoint center, double radius) {
        double distance = distance(point, center);
        return distance <= radius;
    }

    /**
     * 判断点是否在多边形内
     * 实现思路：以被测点为端点，向任意方向作射线，统计该射线与多边形的交点数。如果为奇数，Q在多边形内；如果为偶数，Q在多边形外
     *
     * @param point   点
     * @param polygon 多边形
     */
    public static boolean isInPolygon(GeoPoint point, List<GeoPoint> polygon) {
        int size = polygon.size();
        boolean inside = false;
        for (int i = 0, j = size - 1; i < size; j = i++){
            GeoPoint p1 = polygon.get(i);
            GeoPoint p2 = polygon.get(j);
            // 判断线段两端点是否在射线两侧,射线为y轴
            if ((p1.getLatitude() > point.getLatitude() && p2.getLatitude() <= point.getLatitude()) ||
                    (p1.getLatitude() <= point.getLatitude() && p2.getLatitude() > point.getLatitude())){
                // 线段上与射线 Y 坐标相同的点的 X 坐标
                double x = p1.getLongitude() + (point.getLatitude() - p1.getLatitude()) * (p2.getLongitude() - p1.getLongitude()) / (p2.getLatitude() - p1.getLatitude());
                // 射线穿过多边形的边界
                if (point.getLongitude() < x){
                    inside = !inside;
                }
            }
        }
        return inside;
    }
}