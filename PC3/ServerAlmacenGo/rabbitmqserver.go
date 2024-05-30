package main

import (
	"context"
	"database/sql"
	"fmt"
	"log"
	"strconv"
	"strings"
	"time"

	_ "github.com/go-sql-driver/mysql"
	amqp "github.com/rabbitmq/amqp091-go"
)

// make a product struct
type Product struct {
	ID        int
	Name      string
	Category  string
	Amount    int
	Cost      float64
	CostTotal float64
}

func productsToString(products []Product) string {
	var msg string
	for i := 0; i < len(products); i++ {
		msg += strconv.Itoa(products[i].ID) + "," + products[i].Name + "," + products[i].Category + "," + strconv.Itoa(products[i].Amount) + "," + strconv.FormatFloat(products[i].Cost, 'f', -1, 64) + "," + strconv.FormatFloat(products[i].CostTotal, 'f', -1, 64) + ";"
	}
	msg = msg[:len(msg)-1]
	return msg
}

func sendProducts(db *sql.DB) string {
	consultaF := "SELECT ID_PROD, NAME_PROD, COST FROM products"
	rows, err := db.Query(consultaF)
	if err != nil {
		log.Fatal(err)
	}
	defer rows.Close()

	var id int
	var name string
	var cost float64

	var msg string
	for rows.Next() {
		err := rows.Scan(&id, &name, &cost)
		if err != nil {
			log.Fatal(err)
		}
		msg += strconv.Itoa(id) + "," + name + "," + strconv.FormatFloat(cost, 'f', -1, 64) + ";"
	}
	err = rows.Err()
	if err != nil {
		log.Fatal(err)
	}
	return msg
}

func checkExistence(db *sql.DB, id int, amount int) bool {
	consultaF := "SELECT AMOUNT FROM products WHERE ID_PROD = ?"
	rows, err := db.Query(consultaF, id)
	if err != nil {
		log.Fatal(err)
	}
	defer rows.Close()

	var amountDB int
	for rows.Next() {
		err := rows.Scan(&amountDB)
		if err != nil {
			log.Fatal(err)
		}
	}
	err = rows.Err()
	if err != nil {
		log.Fatal(err)
	}
	return amountDB >= amount
}

func checkAllExistences(db *sql.DB, values []string) bool {
	counter := 0
	for i := 0; i < len(values); i++ {
		prod := strings.Split(values[i], ",")
		id, _ := strconv.Atoi(prod[0])
		amount, _ := strconv.Atoi(prod[1])
		if checkExistence(db, id, amount) {
			counter++
		}
	}
	return counter == len(values)
}

func updateProducts(values []string, db *sql.DB) {
	for i := 0; i < len(values); i++ {
		prod := strings.Split(values[i], ",")
		id, _ := strconv.Atoi(prod[0])
		amount, _ := strconv.Atoi(prod[1])
		consultaF := "UPDATE products SET AMOUNT = AMOUNT - ? WHERE ID_PROD = ?"
		_, err := db.Exec(consultaF, amount, id)
		if err != nil {
			log.Fatal(err)
		}
	}
}

func getDataFromId(db *sql.DB, id int, amount int) Product {
	consultaF := "SELECT * FROM products WHERE ID_PROD = ?"
	rows, err := db.Query(consultaF, id)
	if err != nil {
		log.Fatal(err)
	}
	defer rows.Close()

	var product Product
	for rows.Next() {
		err := rows.Scan(&product.ID, &product.Name, &product.Category, &product.Amount, &product.Cost)
		if err != nil {
			log.Fatal(err)
		}
	}
	err = rows.Err()
	if err != nil {
		log.Fatal(err)
	}
	product.Amount = amount
	return product
}

func sendToJava(db *sql.DB, name string, ruc string, products string, ch *amqp.Channel) string {
	var msg string
	msg = name + ";" + ruc
	temp := strings.Split(products, ",")
	prds := []Product{}
	for i := 0; i < len(temp); i += 2 {
		temp[i] = strings.Trim(temp[i], " ")
		id, _ := strconv.Atoi(temp[i])
		amount, _ := strconv.Atoi(temp[i+1])
		prd := getDataFromId(db, id, amount)
		prd.CostTotal = prd.Cost * float64(prd.Amount)
		prd.CostTotal = float64(int(prd.CostTotal*100)) / 100
		prds = append(prds, prd)
	}
	msg += ";" + productsToString(prds)
	err := ch.Publish(
		"",
		"go-java-queue",
		false,
		false,
		amqp.Publishing{
			ContentType: "text/plain",
			Body:        []byte(msg),
		})
	failOnError(err, "Failed to publish a message to the new queue")
	return "Venta Realizada"
}

func failOnError(err error, msg string) {
	if err != nil {
		log.Panicf("%s: %s", msg, err)
	}
}

func arrayToString(arr []string) string {
	str := strings.Trim(strings.Join(strings.Fields(fmt.Sprint(arr)), ", "), "[]")
	return str
}

func main() {
	db, err := sql.Open("mysql", "root:2206@tcp(127.0.0.1:3306)/storageDB")
	defer db.Close()

	if err != nil {
		log.Fatal(err)
	} else {
		log.Printf("Conectado a la bd")
	}

	conn, err := amqp.Dial("amqp://guest:guest@localhost:5672/venta_host")
	failOnError(err, "Failed to connect to RabbitMQ")
	defer conn.Close()

	ch, err := conn.Channel()
	failOnError(err, "Failed to open the client channel")
	defer ch.Close()

	q, err := ch.QueueDeclare(
		"go-python-queue",
		false,
		false,
		false,
		false,
		nil,
	)
	failOnError(err, "Failed to declare the client queue")
	err = ch.Qos(
		1,
		0,
		false,
	)
	failOnError(err, "Failed to set the client QoS")

	msgs, err := ch.Consume(
		q.Name,
		"",
		false,
		false,
		false,
		false,
		nil,
	)
	failOnError(err, "Failed to register the client consumer")

	ch_java, err := conn.Channel()
	failOnError(err, "Failed to open the java channel")
	defer ch_java.Close()

	var forever chan struct{}
	go func() {
		ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()
		for d := range msgs {
			msg := string(d.Body)
			var response = ""
			if msg == "get_products" {
				response = sendProducts(db)
			} else {
				log.Printf(" [.] Recibido: %v", msg)
				resp := strings.Split(msg, ";")
				values := strings.Split(resp[2], "/")
				if !checkAllExistences(db, values) {
					response = "No se pudo realizar la venta"
				} else {
					updateProducts(values, db)
					sendToJava(db, resp[0], resp[1], arrayToString(values), ch_java)
					response = "Venta Realizada"
				}
			}
			err = ch.PublishWithContext(ctx,
				"",
				d.ReplyTo,
				false,
				false,
				amqp.Publishing{
					ContentType:   "text/plain",
					CorrelationId: d.CorrelationId,
					Body:          []byte(response),
				})
			failOnError(err, "Failed to publish a message")

			d.Ack(false)
		}
	}()

	log.Printf(" [*] Awaiting RPC requests")
	<-forever
}
