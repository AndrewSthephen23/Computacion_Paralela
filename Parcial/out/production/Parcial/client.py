import socket
import threading
class Point:
    def __init__(self, x, y):
        self.x = x
        self.y = y

    def __str__(self):
        return f"({self.x}, {self.y})"

class Client:
    def __init__(self, host, port):
        self.host = host
        self.port = port
        self.points = []
        self.centroids = []
        self.cluster = []

    def run(self):
        try:
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as client:
                client.connect((self.host, self.port))
                print(f"Client has connected to server on port {self.port}")
                receiveDataThread = threading.Thread(target=self.receiveData, args=(client,))
                receiveDataThread.start()
        except Exception as e:
            print(e)
    def receiveData(self, client):
        try:
            with client:
                scanner = client.makefile(mode="r")
                bDataReceived = False
                while True:
                    if not bDataReceived:
                        message = scanner.readline().strip()
                        if message:
                            data = message.split("%")
                            pointsString = data[0].split(";")
                            centroidsString = data[1].split(";")
                            self.parseData(pointsString, self.points)
                            self.parseData(centroidsString, self.centroids)
                            bDataReceived = True
                            self.calculateKMeans(client)
                    else:
                        message = scanner.readline().strip()
                        if message:
                            centroidsString = message.split(";")
                            self.parseData(centroidsString, self.centroids)
                            self.calculateKMeans(client)
        except Exception as e:
            print(e)

    def parseData(self, dataString, dataList):
        dataList.clear()
        for pointString in dataString:
            pointData = pointString.strip('()\n').split(',')
            dataList.append(Point(float(pointData[0]), float(pointData[1])))

    def calculateDistance(self, point, centroid):
        return ((point.x - centroid.x) ** 2 + (point.y - centroid.y) ** 2) ** 0.5

    def calculateKMeans(self, client):
        for point in self.points:
            minDistance = float("inf")
            index = 0
            for i in range(len(self.centroids)):
                distance = self.calculateDistance(point, self.centroids[i])
                if distance < minDistance:
                    minDistance = distance
                    index = i
            self.cluster.append(index + 1)

        message = "[" + ", ".join(str(x) for x in self.cluster) + "]" + "\n"
        client.send(message.encode())
        self.cluster.clear()
        print("Data sent to server")

if __name__ == "__main__":
    host = "localhost"
    port = 2206
    client = Client(host, port)
    client.run()
