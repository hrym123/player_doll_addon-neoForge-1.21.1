# 自定义动作指南

## 方法一：使用 JSON 文件定义动作

### 1. 创建动作 JSON 文件

在 `src/main/resources/assets/player_doll_addon/actions/` 目录下创建 JSON 文件，例如 `my_action.json`：

```json
{
  "name": "my_action",
  "looping": false,
  "keyframes": [
    {
      "tick": 0,
      "pose": "standing"
    },
    {
      "tick": 10,
      "pose": {
        "name": "custom_pose",
        "head": [0, 0, 0],
        "hat": [0, 0, 0],
        "body": [0, 0, 0],
        "rightArm": [-1.5708, 0, 0],
        "leftArm": [1.5708, 0, 0],
        "rightLeg": [0, 0, 0],
        "leftLeg": [0, 0, 0]
      }
    },
    {
      "tick": 20,
      "pose": "standing"
    }
  ]
}
```

### 2. JSON 格式说明

- **name**: 动作名称（可选，用于标识）
- **looping**: 是否循环播放（true/false）
- **keyframes**: 关键帧数组
  - **tick**: 关键帧的时间点（游戏tick，1 tick = 0.05秒）
  - **pose**: 该关键帧的姿态
    - 可以是字符串：引用已定义的姿态文件（如 "standing"）
    - 可以是对象：内联定义姿态

### 3. 姿态定义格式

姿态定义包含以下部分（所有角度单位为弧度）：

- **head**: [x, y, z] 头部旋转
- **hat**: [x, y, z] 帽子旋转
- **body**: [x, y, z] 身体旋转
- **rightArm**: [x, y, z] 右臂旋转
- **leftArm**: [x, y, z] 左臂旋转
- **rightLeg**: [x, y, z] 右腿旋转
- **leftLeg**: [x, y, z] 左腿旋转

### 4. 角度转换

常用角度转换：
- 0度 = 0 弧度
- 45度 = 0.7854 弧度
- 90度 = 1.5708 弧度
- 180度 = 3.1416 弧度

## 方法二：通过代码创建动作

### 1. 创建关键帧

```java
import com.lanye.dolladdon.api.action.ActionKeyframe;
import com.lanye.dolladdon.api.action.SimpleDollAction;
import com.lanye.dolladdon.api.pose.SimpleDollPose;

// 创建关键帧数组
ActionKeyframe[] keyframes = new ActionKeyframe[]{
    // 第0帧：起始姿态
    new ActionKeyframe(0, SimpleDollPose.createDefaultStandingPose()),
    
    // 第10帧：自定义姿态
    new ActionKeyframe(10, new SimpleDollPose(
        "my_pose",
        new float[]{0, 0, 0},        // head
        new float[]{0, 0, 0},        // hat
        new float[]{0, 0, 0},        // body
        new float[]{-1.5708F, 0, 0}, // rightArm (向上90度)
        new float[]{1.5708F, 0, 0},  // leftArm
        new float[]{0, 0, 0},        // rightLeg
        new float[]{0, 0, 0}        // leftLeg
    )),
    
    // 第20帧：回到起始姿态
    new ActionKeyframe(20, SimpleDollPose.createDefaultStandingPose())
};
```

### 2. 创建动作对象

```java
// 创建不循环的动作
DollAction action = new SimpleDollAction("my_action", false, keyframes);

// 创建循环的动作
DollAction loopingAction = new SimpleDollAction("my_looping_action", true, keyframes);
```

### 3. 注册动作

```java
import com.lanye.dolladdon.util.PoseActionManager;

// 注册自定义动作
PoseActionManager.registerAction("my_action", action);
```

## 使用动作

### 在代码中应用动作到实体

```java
import com.lanye.dolladdon.util.PoseActionManager;
import com.lanye.dolladdon.base.entity.BaseDollEntity;

// 获取动作
DollAction action = PoseActionManager.getAction("wave");

// 应用到实体
if (action != null) {
    entity.setAction(action);
}

// 停止动作
entity.stopAction();

// 设置静态姿态
DollPose pose = PoseActionManager.getPose("standing");
entity.setPose(pose);
```

## 示例动作

### 1. 挥手动作（wave.json）

```json
{
  "name": "wave",
  "looping": false,
  "keyframes": [
    {"tick": 0, "pose": "standing"},
    {"tick": 5, "pose": {"rightArm": [-1.5708, 0, 0]}},
    {"tick": 10, "pose": "standing"}
  ]
}
```

### 2. 跳舞动作（dance.json）

```json
{
  "name": "dance",
  "looping": true,
  "keyframes": [
    {
      "tick": 0,
      "pose": {
        "rightArm": [-1.5708, 0, 0],
        "leftArm": [1.5708, 0, 0]
      }
    },
    {
      "tick": 10,
      "pose": {
        "rightArm": [1.5708, 0, 0],
        "leftArm": [-1.5708, 0, 0]
      }
    }
  ]
}
```

### 3. 坐下动作（sit.json）

```json
{
  "name": "sit",
  "looping": false,
  "keyframes": [
    {"tick": 0, "pose": "standing"},
    {
      "tick": 5,
      "pose": {
        "rightLeg": [1.5708, 0, 0],
        "leftLeg": [1.5708, 0, 0]
      }
    }
  ]
}
```

## 提示

1. **关键帧之间的插值**：系统会自动在关键帧之间进行线性插值，使动作更流畅
2. **循环动作**：设置 `looping: true` 可以让动作循环播放
3. **引用姿态**：可以在关键帧中引用已定义的姿态文件，避免重复定义
4. **内联姿态**：可以在关键帧中直接定义姿态，只指定需要改变的部分
5. **时间控制**：使用 `tick` 值控制动作的节奏，1 tick = 0.05秒

## 常见姿态角度参考

- **手臂自然下垂**: [0, 0, 0]
- **手臂向前**: [-0.6981317, 0, 0] (约-40度)
- **手臂向上**: [-1.5708, 0, 0] (90度)
- **手臂水平**: [0, 1.5708, 0] (90度Y轴)
- **腿伸直**: [0, 0, 0]
- **腿弯曲**: [1.5708, 0, 0] (90度)

