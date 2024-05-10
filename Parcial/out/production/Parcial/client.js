const net = require('net');

class Point {
    constructor(x, y) {
        this.x = x;
        this.y = y;
    }
    update(x,y) {
        this.x = x;
        this.y = y;
    }
    toString(){
        return `(${this.x}, ${this.y})`
    }
}

const port = 2206
const host = 'localhost'
const client = new net.Socket();
let points = []
let centroids = []
let cluster = []
let bDataReceived = false;

client.connect(port, host, function() {
    console.log(`Client has connected to server on port ${port}`);
});

client.on('data', (data) => {
    if(!bDataReceived){
        const message = data.toString('utf-8');
        const dataArray = message.split('%');
        const pointsString = dataArray[0].split(';');
        const centroidsString = dataArray[1].split(';');
        points = parseData(pointsString);
        centroids = parseData(centroidsString);
        calculateKMeans();
        bDataReceived = true;
    }else{
        const message = data.toString('utf-8');
        const centroidsString = message.split(';');
        centroids = parseData(centroidsString);
        calculateKMeans();
    }
});

client.on('error', function() {
    console.log('Server closed');
});

function calculateDistance(point, centroid) {
    return Math.sqrt(Math.pow(point.x - centroid.x, 2) + Math.pow(point.y - centroid.y, 2))
}

function calculateKMeans() {
    for (const point of points) {
        let minDistance = Number.MAX_VALUE;
        let index = 0;
        for (let i = 0; i < centroids.length; i++) {
            const distance = calculateDistance(point, centroids[i]);
            if (distance < minDistance) {
                minDistance = distance;
                index = i;
            }
        }
        cluster.push(index + 1);
    }
    client.write(`[${cluster}]\n`);
    cluster = [];
    console.log("Data sent to server");
}

function parseData(dataArray) {
    data = [];
    for (const pointString of dataArray) {
        const pointData = pointString.split(',');
        pointData[0] = pointData[0].substring(1);
        pointData[1] = pointData[1].substring(0, pointData[1].length - 1);
        if(pointData[1][pointData[1].length - 1] == ')') pointData[1] = pointData[1].substring(0, pointData[1].length - 1);
        data.push(new Point(parseFloat(pointData[0]), parseFloat(pointData[1])));
    }
    return data;
}