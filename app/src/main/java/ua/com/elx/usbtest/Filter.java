package ua.com.elx.usbtest;

/**
 * Created by michaelbilenko on 11.10.14.
 */
public class Filter {
    static double NOMINAL_FRICTION=0.97;
    double mass = 5; //kg
    double friction = 0.005;
    double speed=0.0,position=0.0;
    long startTime;

    public void setFriction(double f) {
        this.friction = f;
    }


    public Filter(double mass,double friction)
    {
        setCoef(mass,friction);
    }

    public void setCoef(double mass,double friction) {

        this.mass = mass;
        this.friction = friction;
        startTime = System.currentTimeMillis();
    }

    public double doFilter(double acceleration) {

        acceleration = acceleration;

        double t = ((double)(System.currentTimeMillis()-startTime)/1000);
        startTime = System.currentTimeMillis();

        speed = (float) (mass * acceleration * t + speed);
        position += mass * acceleration / 2 * t * t + speed * t;
        speed *= friction;

        if (position > 0) {
            speed=-0.05f;

            position=0;
        }

        return position;

    }

}
