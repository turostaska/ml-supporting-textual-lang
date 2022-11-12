package modelvisualizer

enum class LayerType {
    Conv1d, Conv2d, Conv3d,
    BatchNorm1d, BatchNorm2d, BatchNorm3d,
    MaxPool1d, MaxPool2d, MaxPool3d,
    Linear,
    ReLU,
    Sequential,
}

fun LayerType.isMaxPool() = this.name.startsWith("MaxPool")

interface ILayer {
    val type: LayerType
    val inChannels: Int
    val outChannels: Int
}

class Layer(
    override val type: LayerType,
    override val inChannels: Int,
    override val outChannels: Int,
) : ILayer

class MaxPoolLayer(
    override val type: LayerType,
) : ILayer {
    // Dummy override, the previous and next layers define the number of channels
    override val inChannels get() = -1
    override val outChannels get() = -1

    init {
        require(type.isMaxPool())
    }
}

class ReluLayer : ILayer {
    // Dummy override, the previous and next layers define the number of channels
    override val inChannels get() = -1
    override val outChannels get() = -1
    override val type: LayerType get() = LayerType.ReLU
}

class SequentialLayer(
    val layers: List<ILayer>,
): ILayer {
    override val type: LayerType get() = LayerType.Sequential
    override val inChannels: Int get() = layers.first().inChannels
    override val outChannels: Int get() = layers.first().outChannels
}
