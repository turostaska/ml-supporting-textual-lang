package modelvisualizer

enum class LayerType {
    Conv1d, Conv2d, Conv3d,
    BatchNorm1d, BatchNorm2d, BatchNorm3d,
    MaxPool1d, MaxPool2d, MaxPool3d,
    Linear,
    ReLU,
}

fun LayerType.isMaxPool() = this.name.startsWith("MaxPool")

class Layer(
    val type: LayerType,
    val inChannels: Int,
    val outChannels: Int,
)
