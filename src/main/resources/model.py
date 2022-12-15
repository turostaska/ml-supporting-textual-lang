import torch.nn as nn
import torch.nn.functional as F


class CNN(nn.Module):
    def __init__(self):
        super(CNN, self).__init__()
        self.conv1 = nn.Conv3d(1, 4, kernel_size=3, padding=1)
        self.bn1 = nn.BatchNorm3d(4)
        self.pool1 = nn.MaxPool3d(kernel_size=(1, 2, 2))

        self.conv2 = nn.Conv3d(4, 8, kernel_size=3, padding=1)
        self.bn2 = nn.BatchNorm3d(8)
        self.pool2 = nn.MaxPool3d(kernel_size=2)

        self.conv3a = nn.Conv3d(8, 12, kernel_size=3, padding=1)
        self.conv3b = nn.Conv3d(12, 12, kernel_size=3, padding=1)
        self.bn3 = nn.BatchNorm3d(12)
        self.pool3 = nn.MaxPool3d(kernel_size=2)

        self.conv4a = nn.Conv3d(12, 16, kernel_size=3, padding=1)
        self.conv4b = nn.Conv3d(16, 16, kernel_size=3, padding=1)
        self.bn4 = nn.BatchNorm3d(16)
        self.pool4 = nn.MaxPool3d(kernel_size=2)

        self.conv5a = nn.Conv3d(16, 16, kernel_size=3, padding=1)
        self.conv5b = nn.Conv3d(16, 16, kernel_size=3, padding=1)
        self.bn5 = nn.BatchNorm3d(16)
        self.pool5 = nn.MaxPool3d(kernel_size=(1, 2, 2))

        self.fc6 = nn.Linear(16, 4)
        self.fc7 = nn.Linear(4, 1)

    def forward(self, x):
        x = self.pool1(F.relu(self.conv1(x)))
        x = self.bn1(x)

        x = self.pool2(F.relu(self.conv2(x)))
        x = self.bn2(x)

        x = F.relu(self.conv3a(x))
        x = self.pool3(F.relu(self.conv3b(x)))
        x = self.bn3(x)

        x = F.relu(self.conv4a(x))
        x = self.pool4(F.relu(self.conv4b(x)))
        x = self.bn4(x)

        x = F.relu(self.conv5a(x))
        x = self.pool5(F.relu(self.conv5b(x)))
        x = self.bn5(x)

        x = x.view(-1, 4192)
        x = F.relu(self.fc6(x))

        x = self.fc7(x)

        return x

