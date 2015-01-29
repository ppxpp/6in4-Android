package cn.edu.bupt.niclab.event;

/**
 * Created by zhengmeng on 2015/1/19.
 */
public class BaseEvent {

    public static final int EVENT_SUCCESS = 1;
    public static final int EVENT_FAILED = 2;
    public static final int EVENT_CANCEL = 3;
    public static final int EVENT_NULL = 4;
    
    protected int mResult;
    
    public BaseEvent(){
        mResult = EVENT_NULL;
    }
    
    public BaseEvent(int result){
        if (result != EVENT_SUCCESS 
                && result != EVENT_FAILED
                && result != EVENT_CANCEL
                && result != EVENT_NULL){
            throw new IllegalArgumentException("BaseEvent, Illegal result");
        }
        mResult = result;
    }
    
    public int getResult(){
        return mResult;
    }

}
