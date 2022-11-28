from torch import nn # Import the nn sub-module from PyTorch

class NeuralNetwork(nn.Module): # Neural networks are defined as classes
    def __init__(self): # Layers and variables are defined in the __init__ method
        super(NeuralNetwork, self).__init__() # Must be in every network.
        self.flatten = nn.Flatten() # Defining a flattening layer.
        self.linear_relu_stack = nn.Sequential( # Defining a stack of layers.
            nn.Linear(2, 6), # Linear Layers have an input and output shape
            nn.ReLU(), # ReLU is one of many activation functions provided by nn
            nn.Linear(6, 4),
            nn.ReLU(),
            nn.Linear(4, 3),
        )

    def forward(self, x): # This function defines the forward pass.
        x = self.flatten(x)
        x = self.linear_relu_stack(x)
        return x
