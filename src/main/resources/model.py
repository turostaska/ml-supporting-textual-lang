import torch.nn as nn
import torch.nn.functional as F


class CNN(nn.Module):
    def __init__(self):
        super(CNN, self).__init__()
        self.conv1 = nn.Conv3d(1, 64, kernel_size=3, padding=1)
        self.bn1 = nn.BatchNorm3d(64)
        self.pool1 = nn.MaxPool3d(kernel_size=(1, 2, 2))

        self.conv2 = nn.Conv3d(64, 128, kernel_size=3, padding=1)
        self.bn2 = nn.BatchNorm3d(128)
        self.pool2 = nn.MaxPool3d(kernel_size=2)

        self.conv3a = nn.Conv3d(128, 256, kernel_size=3, padding=1)
        self.conv3b = nn.Conv3d(256, 256, kernel_size=3, padding=1)
        self.bn3 = nn.BatchNorm3d(256)
        self.pool3 = nn.MaxPool3d(kernel_size=2)

        self.conv4a = nn.Conv3d(256, 512, kernel_size=3, padding=1)
        self.conv4b = nn.Conv3d(512, 512, kernel_size=3, padding=1)
        self.bn4 = nn.BatchNorm3d(512)
        self.pool4 = nn.MaxPool3d(kernel_size=2)

        self.conv5a = nn.Conv3d(512, 512, kernel_size=3, padding=1)
        self.conv5b = nn.Conv3d(512, 512, kernel_size=3, padding=1)
        self.bn5 = nn.BatchNorm3d(512)
        self.pool5 = nn.MaxPool3d(kernel_size=(1, 2, 2))

        self.fc6 = nn.Linear(8192, 4096)  # todo: kiszámolni az input layerek számát
        self.fc7 = nn.Linear(4096, 1)

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

        x = x.view(-1, 8192)
        x = F.relu(self.fc6(x))

        x = self.fc7(x)

        return x

