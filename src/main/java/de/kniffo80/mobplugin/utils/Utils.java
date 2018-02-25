/**
 * Utils.java
 * 
 * Created on 10:18:38
 */
package de.kniffo80.mobplugin.utils;

import cn.nukkit.Server;
import cn.nukkit.math.Vector3;
import cn.nukkit.utils.TextFormat;

import java.util.Random;

/**
 * @author <a href="mailto:kniffman@googlemail.com">Michael Gertz (kniffo80)</a>
 *
 */
public class Utils {
    
    private static final Server SERVER = Server.getInstance();
    
    public static final void logServerInfo (String text) {
        SERVER.getLogger().info(TextFormat.GOLD + "[MobPlugin] " + text);
    }
    
    private static final Random random = new Random(System.currentTimeMillis());

    /**
     * Returns a random number between min (inkl.) and max (excl.) If you want a number between 1 and 4 (inkl) you need to call rand (1, 5)
     * 
     * @param min min inklusive value
     * @param max max exclusive value
     * @return
     */
    public static int rand(int min, int max) {
        if (min == max) {
            return max;
        }
        return min + random.nextInt(max - min);
    }

    /**
     * Returns random boolean
     * @return  a boolean random value either <code>true</code> or <code>false</code>
     */
    public static boolean rand() {
        return random.nextBoolean();
    }

    public static final int ACCORDING_X_OBTAIN_Y = 0;
    public static final int ACCORDING_Y_OBTAIN_X = 1;

    /**
     * 一次函数求解 根据X求Y或根据Y求X<br>
     *     如果两个{@code Vector3} 的 {@link Vector3#getY()} 不同则返回 {@link Double#MAX_VALUE}
     * @param pos1 坐标1
     * @param pos2 坐标2
     * @param element 输入的X或者Y
     * @param type {@link Utils#ACCORDING_X_OBTAIN_Y} 或者 {@link Utils#ACCORDING_Y_OBTAIN_X}
     * @return 结果 如果无法得出结果则返回{@link Double#MAX_VALUE}
     */
    public static double calLinearFunction(Vector3 pos1, Vector3 pos2, double element, int type) {
        if(pos1.getFloorY() == pos2.getFloorY())return Double.MAX_VALUE;
        if(pos1.getX() == pos2.getX()){
            if(type == ACCORDING_Y_OBTAIN_X) return pos1.getX();
            else return Double.MAX_VALUE;
        }else if(pos1.getY() == pos2.getY()){
            if(type == ACCORDING_X_OBTAIN_Y) return pos1.getY();
            else return Double.MAX_VALUE;
        }else{
            if(type == ACCORDING_X_OBTAIN_Y){
                //Y = [(x-x1)(y1-y2)/(x1-x2) ] + y1
                return (((element-pos1.getX())*(pos1.getY()-pos2.getY()))/(pos1.getX()-pos2.getX())) + pos1.getY();
            }else{//ACCORDING_Y_OBTAIN_X
                //X = [(y-y1)(x1-x2)]/(y1-y2) + x1
                return (((element-pos1.getY())*(pos1.getX()-pos2.getX()))/(pos1.getY()-pos2.getY())) + pos1.getX();
            }
        }
    }

}
