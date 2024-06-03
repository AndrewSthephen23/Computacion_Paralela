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

func main() {
	conn, err := amqp.Dial("amqp://admin:admin@localhost:5672/viajes_host")
	failOnError(err, "Failed to connect to RabbitMQ")
	defer conn.Close()

	ch, err := conn.Channel()
	failOnError(err, "Failed to open a channel")
	defer ch.Close()

	q, err := ch.QueueDeclare(
		"python-go-queuecon",
		false,
		false,
		false,
		false,
		nil,
	)
	failOnError(err, "Failed to declare a queue")

	msgs, err := ch.Consume(
		q.Name,
		"",
		true,
		false,
		false,
		false,
		nil,
	)
	failOnError(err, "Failed to register a consumer")
}

func devolverMensaje(mensaje string){
	conn, err := amqp.Dial("amqp://admin:admin@localhost:5672/viajes_host")
	failOnError(err, "Failed to connect to RabbitMQ")
	defer conn.Close()

	ch, err := conn.Channel()
	failOnError(err, "Failed to open a channel")
	defer ch.Close()

	q, err := ch.QueueDeclare(
		"go-python-queueconfirm",
		false,
		false,
		false,
		false,
		nil,
	)
	failOnError(err, "Failed to declare a queue")

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	body := mensaje
	err = ch.PublishWithContext(ctx,
		"",
		q.Name,
		false,
		false,
		amqp.Publishing{
			ContentType: "text/plain",
			Body:        []byte(body),
		})
	failOnError(err, "Failed to publish a message")
}

func Purchase(origen string, destino string, cantAsientos int){
	enough, err:= canPurchase(origen, destino, cantAsientos)
	if err != nil {
		fmt.Println(err)
		return
	}
	if enough{
		db, err := sql.Open("mysql", "concurrente:1234@tcp(127.0.0.1:3306)/bd_rutas")
		if err != nil {
			fmt.Println(err)
			return
		}
		defer db.Close()
		query := "SELECT id_rutas FROM rutas WHERE origen = ? AND destino = ?"
		var id_rutas int
		err = db.QueryRow(query, origen, destino).Scan(&id_rutas)
		if err != nil {
			panic(err.Error())
		}

		_, err = db.Exec("UPDATE rutas SET cant_asientos = cant_asientos - ? WHERE id_rutas = ?", cantAsientos, id_rutas)
		if err != nil{
			fmt.Error("canPurchase: %d: %v",id_rutas,err)
		}

		query2 := "SELECT precio FROM rutas WHERE id_rutas = ?"
		var precio float64
		err = db.QueryRow(query2, id_rutas).Scan(&precio)
		if err != nil {
			panic(err.Error())
		}
		fmt.Println("Pasajes Disponibles")
			devolverMensaje(fmt.Sprintf("%s,%s,%d,%f",origen,destino,cantAsientos,precio))
	}else{
		fmt.Println("Pasajes Agotados")
		devolverMensaje(0)
	}
	
}

func canPurchase(origen string, destino string, cantAsientos int) (bool, error) {
	db, err := sql.Open("mysql", "concurrente:1234@tcp(127.0.0.1:3306)/bd_rutas")
	if err != nil {
		panic(err.Error())
	}
	defer db.Close()
	
	var enough bool
	err = db.QueryRow("SELECT (cant_asientos >= ?) FROM rutas WHERE origen = ? AND destino = ?",cantidad, origen, destino).Scan(&enough)
	if err != nil {
		if err == sql.ErrNoRows {
			return false, fmt.Errorf("canPurchase from %s to %s: unknown route",origen,destino)
		}
		return false, fmt.Errorf("canPurchase from %s to %s: %v",origen,destino,err)
	}
	return enough, nil
}