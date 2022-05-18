package dev.lone.blocksinjector;

import java.lang.reflect.Field;

public class Nms
{
    public static Field getField(Class<?> clazz, Class<?> type, int index)
    {
        int i = 0;
        for (Field field : clazz.getDeclaredFields())
        {
            if (field.getType() == type)
            {
                i++;
                if(index == i)
                {
                    field.setAccessible(true);
                    return field;
                }
            }
        }
        return null;
    }
}
