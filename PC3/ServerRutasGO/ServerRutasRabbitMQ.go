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

// Estructura que representa una ruta.
type Ruta struct {
	ID         int
	NameRuta   string
	DesdeLugar string
	HastaLugar string
	Unit       int
	Amount     int
	Cost       float64
	IDBus      int
}

// Convierte una lista de Rutas en una cadena de texto.
func rutasToString(rutas []Ruta) string {
	var msg string
	for i := 0; i < len(rutas); i++ {
		msg += strconv.Itoa(rutas[i].ID) + "," + rutas[i].NameRuta + "," + rutas[i].DesdeLugar + "," + rutas[i].HastaLugar + "," + strconv.Itoa(rutas[i].Unit) + "," + strconv.Itoa(rutas[i].Amount) + "," + strconv.FormatFloat(rutas[i].Cost, 'f', -1, 64) + "," + strconv.Itoa(rutas[i].IDBus) + ";"
	}
	msg = msg[:len(msg)-1] // Elimina el ; del final
	return msg
}

// Envia las rutas de la base de datos como una cadena de texto.
func sendRutas(db *sql.DB) string {
	consultaF := "SELECT id_RUTA, name_ruta, desde_lugar, hasta_lugar, unit, amount, cost, id_bus FROM rutas"
	rows, err := db.Query(consultaF)
	if err != nil {
		log.Fatal(err)
	}
	defer rows.Close()

	var id int
	var nameRuta, desdeLugar, hastaLugar string
	var unit, amount, idBus int
	var cost float64

	var msg string
	for rows.Next() {
		err := rows.Scan(&id, &nameRuta, &desdeLugar, &hastaLugar, &unit, &amount, &cost, &idBus)
		if err != nil {
			log.Fatal(err)
		}
		msg += strconv.Itoa(id) + "," + nameRuta + "," + desdeLugar + "," + hastaLugar + "," + strconv.Itoa(unit) + "," + strconv.Itoa(amount) + "," + strconv.FormatFloat(cost, 'f', -1, 64) + "," + strconv.Itoa(idBus) + ";"
	}
	err = rows.Err()
	if err != nil {
		log.Fatal(err)
	}
	return msg
}

// Verifica la existencia de una cantidad específica de una ruta
func checkExistence(db *sql.DB, id int, amount int) bool {
	consultaF := "SELECT amount FROM rutas WHERE id_RUTA = ?"
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

// Verifica la existencia de todas las cantidades de rutas especificadas.
func checkAllExistences(db *sql.DB, values []string) bool {
	counter := 0
	for i := 0; i < len(values); i++ {
		ruta := strings.Split(values[i], ",")
		id, _ := strconv.Atoi(ruta[0])
		amount, _ := strconv.Atoi(ruta[1])
		if checkExistence(db, id, amount) {
			counter++
		}
	}
	return counter == len(values)
}

// Actualiza las cantidades de las rutas en la base de datos.
func updateRutas(values []string, db *sql.DB) {
	for i := 0; i < len(values); i++ {
		ruta := strings.Split(values[i], ",")
		id, _ := strconv.Atoi(ruta[0])
		amount, _ := strconv.Atoi(ruta[1])
		consultaF := "UPDATE rutas SET amount = amount - ? WHERE id_RUTA = ?"
		_, err := db.Exec(consultaF, amount, id)
		if err != nil {
			log.Fatal(err)
		}
	}
}

// Obtiene los datos de una ruta a partir de su ID y la cantidad.
func getDataFromId(db *sql.DB, id int, amount int) Ruta {
	consultaF := "SELECT id_RUTA, name_ruta, desde_lugar, hasta_lugar, unit, amount, cost, id_bus FROM rutas WHERE id_RUTA = ?"
	rows, err := db.Query(consultaF, id)
	if err != nil {
		log.Fatal(err)
	}
	defer rows.Close()

	var ruta Ruta
	for rows.Next() {
		err := rows.Scan(&ruta.ID, &ruta.NameRuta, &ruta.DesdeLugar, &ruta.HastaLugar, &ruta.Unit, &ruta.Amount, &ruta.Cost, &ruta.IDBus)
		if err != nil {
			log.Fatal(err)
		}
	}
	err = rows.Err()
	if err != nil {
		log.Fatal(err)
	}
	ruta.Amount = amount
	return ruta
}

// Envia los datos de la venta a un servicio Java a través de RabbitMQ.
func sendToJava(db *sql.DB, name string, ruc string, products string, ch *amqp.Channel) string {
	var msg string
	msg = name + ";" + ruc
	temp := strings.Split(products, ",")
	rts := []Ruta{}
	for i := 0; i < len(temp); i += 2 {
		temp[i] = strings.Trim(temp[i], " ")
		id, _ := strconv.Atoi(temp[i])
		amount, _ := strconv.Atoi(temp[i+1])
		rt := getDataFromId(db, id, amount)
		rts = append(rts, rt)
	}
	msg += ";" + rutasToString(rts)
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

// Maneja los errores registrándolos y deteniendo la ejecución si es necesario.
func failOnError(err error, msg string) {
	if err != nil {
		log.Panicf("%s: %s", msg, err)
	}
}

// Convierte un array de strings en una cadena de texto, separada por comas.
func arrayToString(arr []string) string {
	str := strings.Trim(strings.Join(strings.Fields(fmt.Sprint(arr)), ", "), "[]")
	return str
}

// Función principal
func main() {
	// Conexión a la base de datos MySQL.
	db, err := sql.Open("mysql", "concurrente:1234@tcp(127.0.0.1:3306)/bd_rutas")
	defer db.Close()

	if err != nil {
		log.Fatal(err)
	} else {
		log.Printf("Conectado a la bd")
	}

	// Conexión a RabbitMQ
	conn, err := amqp.Dial("amqp://admin:admin@localhost:5672/viajes_host")
	failOnError(err, "Failed to connect to RabbitMQ")
	defer conn.Close()

	// Canal de comunicaciones con RabbitMQ.
	ch, err := conn.Channel()
	failOnError(err, "Failed to open the client channel")
	defer ch.Close()

	// Declaración de la cola para recibir solicitudes.
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

	// Registro del consumidor para recibir mensajes de la cola.
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

	// Canal adicional para comunicarse con el servicio Java.
	ch_java, err := conn.Channel()
	failOnError(err, "Failed to open the java channel")
	defer ch_java.Close()

	// Canal de comunicación para mantener el programa en ejecución.
	var forever chan struct{}
	go func() {
		// Contexto con tiempo de espera para las solicitudes
		ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()
		for d := range msgs {
			msg := string(d.Body)
			var response = ""
			if msg == "get_rutas" {
				response = sendRutas(db)
			} else {
				log.Printf(" [.] Recibido: %v", msg)
				resp := strings.Split(msg, ";")
				values := strings.Split(resp[2], "/")
				if !checkAllExistences(db, values) {
					response = "No se pudo realizar la venta"
				} else {
					updateRutas(values, db)
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
