package sample;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.*;

public class Main extends Application{

    @Override
    public void start(Stage primaryStage){
        BorderPane root = new BorderPane();
        primaryStage.setTitle("Clipping");
        primaryStage.setScene(new Scene(root, 700, 500));

        Canvas canvas = new Canvas(700, 500);
        root.setCenter(canvas);

        GraphicsContext g = canvas.getGraphicsContext2D();

        g.setLineWidth(3);

        List<Map.Entry<Double, Double>> p1 = new ArrayList<>();
        p1.add(new AbstractMap.SimpleEntry<>(50.0, 90.0));
        p1.add(new AbstractMap.SimpleEntry<>(200.0, 350.0));
        p1.add(new AbstractMap.SimpleEntry<>(320.0, 250.0));
        p1.add(new AbstractMap.SimpleEntry<>(350.0, 170.0));
        p1.add(new AbstractMap.SimpleEntry<>(250.0, 60.0));

        for(int i = 0; i < p1.size(); i++){
            g.strokeLine(
                    p1.get(i).getKey(),
                    p1.get(i).getValue(),
                    p1.get((i + 1) % p1.size()).getKey(),
                    p1.get((i + 1) % p1.size()).getValue());
        }

        List<Map.Entry<Double, Double>> p2 = new ArrayList<>();
        p2.add(new AbstractMap.SimpleEntry<>(250.0, 90.0));
        p2.add(new AbstractMap.SimpleEntry<>(300.0, 350.0));
        p2.add(new AbstractMap.SimpleEntry<>(520.0, 250.0));
        p2.add(new AbstractMap.SimpleEntry<>(550.0, 170.0));
        p2.add(new AbstractMap.SimpleEntry<>(450.0, 60.0));

        for(int i = 0; i < p2.size(); i++){
            g.strokeLine(
                    p2.get(i).getKey(),
                    p2.get(i).getValue(),
                    p2.get((i + 1) % p2.size()).getKey(),
                    p2.get((i + 1) % p2.size()).getValue());
        }

        g.setStroke(Color.RED);

        Set<Map.Entry<Double, Double>> intersection1 = getIntersection(p1, p2);
        Set<Map.Entry<Double, Double>> intersection2 = getIntersection(p2, p1);
        List<Map.Entry<Double, Double>> intersection = new ArrayList<>(intersection2);
        Collections.reverse(intersection);
        intersection1.addAll(intersection);
        intersection.clear();
        intersection.addAll(intersection1);
        for(int i = 0; i < intersection.size(); i++){
            g.strokeLine(
                    intersection.get(i).getKey(),
                    intersection.get(i).getValue(),
                    intersection.get((i + 1) % intersection.size()).getKey(),
                    intersection.get((i + 1) % intersection.size()).getValue());
        }

        primaryStage.show();
    }
    
    static boolean isEqual(double a, double b){
        return Math.abs(a - b) < 1e-5;
    }

    static Set<Map.Entry<Double, Double>> getIntersection(List<Map.Entry<Double, Double>> a, List<Map.Entry<Double, Double>> b){
        Set<Map.Entry<Double, Double>> p = new LinkedHashSet<>();

        for(int i = 0; i < a.size(); i++){
            if(isPointInPolygon(a.get(i), b)){
                p.add(a.get(i));
            }

            p.addAll(getIntersectionPoints(a.get(i), a.get((i+1)%a.size()), b));

            if(isPointInPolygon(a.get((i+1)%a.size()), b)){
                p.add(a.get((i+1)%a.size()));
            }
        }

        return p;
    }

    static Map.Entry<Double, Double> getIntersectionPoint(Map.Entry<Double, Double> l1p1, Map.Entry<Double, Double> l1p2, Map.Entry<Double, Double> l2p1, Map.Entry<Double, Double> l2p2){
        //0,0 2,2 | 1,0 3,2
        double A1 = l1p2.getValue() - l1p1.getValue();
        double B1 = l1p1.getKey() - l1p2.getKey();
        double C1 = A1 * l1p1.getKey() + B1 * l1p1.getValue();

        double A2 = l2p2.getValue() - l2p1.getValue();
        double B2 = l2p1.getKey() - l2p2.getKey();
        double C2 = A2 * l2p1.getKey() + B2 * l2p1.getValue();

        //lines are parallel
        double det = A1 * B2 - A2 * B1;
        if(isEqual(det, 0)){ //MathUtils.isEqual()
            return null; //parallel lines
        } else {
            double x = (B2 * C1 - B1 * C2) / det;
            double y = (A1 * C2 - A2 * C1) / det;
            boolean online1 = ((Math.min(l1p1.getKey(), l1p2.getKey()) < x || isEqual(Math.min(l1p1.getKey(), l1p2.getKey()), x))
                    && (Math.max(l1p1.getKey(), l1p2.getKey()) > x || isEqual(Math.max(l1p1.getKey(), l1p2.getKey()), x))
                    && (Math.min(l1p1.getValue(), l1p2.getValue()) < y || isEqual(Math.min(l1p1.getValue(), l1p2.getValue()), y))
                    && (Math.max(l1p1.getValue(), l1p2.getValue()) > y || isEqual(Math.max(l1p1.getValue(), l1p2.getValue()), y))
            );
            boolean online2 = ((Math.min(l2p1.getKey(), l2p2.getKey()) <= x || isEqual(Math.min(l2p1.getKey(), l2p2.getKey()), x))
                    && (Math.max(l2p1.getKey(), l2p2.getKey()) > x || isEqual(Math.max(l2p1.getKey(), l2p2.getKey()), x))
                    && (Math.min(l2p1.getValue(), l2p2.getValue()) < y || isEqual(Math.min(l2p1.getValue(), l2p2.getValue()), y))
                    && (Math.max(l2p1.getValue(), l2p2.getValue()) > y || isEqual(Math.max(l2p1.getValue(), l2p2.getValue()), y))
            );

            if(online1 && online2)
                return new AbstractMap.SimpleEntry<>(x, y);
        }
        return null; //intersection is at out of at least one segment.
    }

    static List<Map.Entry<Double, Double>> getIntersectionPoints(Map.Entry<Double, Double> p1, Map.Entry<Double, Double> p2, List<Map.Entry<Double, Double>> poly){
        //if line intersects polygon in corner point, function adds this point twice,
        //because two edges of polygon contain this point

        //Set is used to predict adding equal points
        List<Map.Entry<Double, Double>> intersectionPoints = new ArrayList<>();

        for(int i = 0; i < poly.size(); i++){

            int next = (i + 1 == poly.size()) ? 0 : i + 1;

            Map.Entry<Double, Double> ip = getIntersectionPoint(p1, p2, poly.get(i), poly.get(next));

            if(ip != null) intersectionPoints.add(ip);

        }

        return intersectionPoints;
    }


    static boolean isPointInPolygon(Map.Entry<Double, Double> p, List<Map.Entry<Double, Double>> polygon){
        double minX = polygon.get(0).getKey();
        double maxX = polygon.get(0).getKey();
        double minY = polygon.get(0).getValue();
        double maxY = polygon.get(0).getValue();
        for(int i = 1; i < polygon.size(); i++){
            Map.Entry<Double, Double> q = polygon.get(i);
            minX = Math.min(q.getKey(), minX);
            maxX = Math.max(q.getKey(), maxX);
            minY = Math.min(q.getValue(), minY);
            maxY = Math.max(q.getValue(), maxY);
        }

        if(p.getKey() < minX || p.getKey() > maxX || p.getValue() < minY || p.getValue() > maxY){
            return false;
        }

        // http://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
        boolean inside = false;
        for(int i = 0, j = polygon.size() - 1; i < polygon.size(); j = (i++)){
            if((polygon.get(i).getValue() > p.getValue()) != (polygon.get(j).getValue() > p.getValue()) &&
                    p.getKey() < (polygon.get(j).getKey() - polygon.get(i).getKey()) * (p.getValue() - polygon.get(i).getValue()) / (polygon.get(j).getValue() - polygon.get(i).getValue()) + polygon.get(i).getKey()){
                inside = !inside;
            }
        }
        return inside;
    }

    public static void main(String[] args){
        launch(args);
    }
}
