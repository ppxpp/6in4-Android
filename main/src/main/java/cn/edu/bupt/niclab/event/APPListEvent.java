package cn.edu.bupt.niclab.event;

/**
 * Created by zhengmeng on 2015/1/19.
 */
public class APPListEvent extends BaseEvent {
    //加载完app列表后的事件

    public APPListEvent(int result) {
        super(result);
    }
}
