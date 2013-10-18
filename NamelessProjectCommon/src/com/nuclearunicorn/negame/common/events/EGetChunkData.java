package com.nuclearunicorn.negame.common.events;

import com.nuclearunicorn.libroguelike.events.network.NetworkEvent;
import org.lwjgl.util.Point;

/**
 * @author Bloodrizer
 */
public class EGetChunkData extends NetworkEvent {
    private Point chunkOrigin = new Point();

    public EGetChunkData(int x, int y){
        chunkOrigin.setLocation(x, y);
    }

    @Override
    public String[] serialize(){
        return new String[] {
                get_id(),
                //"2",    //meens movement with destination
                Integer.toString(this.chunkOrigin.getX()),
                Integer.toString(this.chunkOrigin.getY())
        };
    }
}
