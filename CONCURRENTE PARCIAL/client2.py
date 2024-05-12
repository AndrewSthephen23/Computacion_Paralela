import socket
import threading
from sklearn.tree import DecisionTreeClassifier
from sklearn.preprocessing import StandardScaler

class Person:
    def __init__(self, id, edad, sexo, peso, altura):
        self.id = id
        self.edad = edad
        self.sexo = sexo
        self.peso = peso
        self.altura = altura

class Client:
    def __init__(self, host, port):
        self.host = host
        self.port = port
        self.persons = []
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
                            personsString = data[0].split(";")
                            centroidsString = data[1].split(";")
                            self.parseData(personsString, self.persons)
                            self.parseData(centroidsString, self.centroids)
                            bDataReceived = True
                            self.calculateMortalityRisk(client)
                    else:
                        message = scanner.readline().strip()
                        if message:
                            centroidsString = message.split(";")
                            self.parseData(centroidsString, self.centroids)
                            self.calculateMortalityRisk(client)
        except Exception as e:
            print(e)

    def parseData(self, dataString, dataList):
        dataList.clear()
        for personString in dataString:
            personData = personString.strip('()\n').split(',')
            person = Person(int(personData[0]), int(personData[1]), int(personData[2]), int(personData[3]), int(personData[4]))
            dataList.append(person)

    def calculateMortalityRisk(self, client):
        # Prepare data for training the decision tree
        X = [[p.edad, p.sexo, p.peso, p.altura] for p in self.persons]
        y = [0] * len(self.persons)  # 0 for low risk, 1 for high risk

        # Train the decision tree
        sc = StandardScaler()
        X = sc.fit_transform(X)
        clf = DecisionTreeClassifier(random_state=42)
        clf.fit(X, y)

        # Predict the risk for each person
        for person in self.persons:
            risk = clf.predict([[person.edad, person.sexo, person.peso, person.altura]])
            self.cluster.append(risk[0])

        # Send the results to the server
        message = "[" + ", ".join(str(x) for x in self.cluster) + "]" + "\n"
        client.send(message.encode())
        self.cluster.clear()
        print("Data sent to server")

if __name__ == "__main__":
    host = "localhost"
    port = 2206
    client = Client(host, port)
    client.run()