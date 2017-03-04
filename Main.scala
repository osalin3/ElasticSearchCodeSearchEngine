//import akka.actor.Status.Success
import scala.util.{Failure, Success}
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.fasterxml.jackson.databind.JsonNode

import scala.xml._
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import com.sksamuel.elastic4s.ElasticDsl._
import org.elasticsearch.common.settings.Settings
import play.libs.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.Failure
import akka.pattern.ask
import net.liftweb.json.DefaultFormats
import net.liftweb.json._

case class Params(field: String, parameter: String, other: ActorRef)
case class URL(url: String)
case class MessagePassed(response: JsonNode)

//gets the JSON
class getJSON extends Actor{
  def receive = {
    case URL(url) =>
      //println("url: " + url)
      val res = scala.io.Source.fromURL(url).mkString
      var json = Json.parse(res) //return this to the calling method
      //println(json)
      sender ! MessagePassed(json)
  }
}

//takes query arguments and returns the JSONfpre
class reqQuery extends Actor{
  def receive = {
    case Params(field, parameter, other) =>{
      //val res = scala.io.Source.fromURL(s"http://104.154.100.222:9200/tproj/proj/_search?q=${field}:${parameter}").mkString
      //var json = Json.parse(res)
      //form the url with the arguments given
      val res = s"http://104.154.100.222:9200/tproj/proj/_search?q=${field}:${parameter}" //pass this to getJSON
      //get the repose back from getJSON
      //print out the response to the query
      val test = other ! URL(res) //send OTHER actor a message to process this job
      //val f //make a future here and try to listed to it here and the also listen to it in another case
      //NOTE: the future was being returned when val f = future.... here f was being assigned onSucess when the future
      // returns, probably need to figure out how it actually works.
    }
    case MessagePassed(response) => //this is the message OTHER actor passes back with a JSON response for query on line 43
    {
      println(Json.toJson(response))
    }
  }
}

class XMLparser extends Actor{
  def receive =
  {
    case _ =>
      println("Entering the XMLparse function")
      for (i <- 27001 to 27001) {
        //String interpolator to replace proj index "projNum"
        try {
          val theUrl = s"https://www.openhub.net/p/$i.xml?api_key=295b223840f8dd20e650504b6950ab6dc28ded04a2a2271f60e6f3efc4b6c3b3"
          val newString = scala.io.Source.fromURL(theUrl).mkString
          val xml = XML.loadString(newString)
          val gitPattern = "github.com".r
          val downloadURL = xml \ "result" \ "project" \ "download_url"
          val match1 = gitPattern.findFirstIn(downloadURL.text)
          //println(downloadURL.text)
          if (match1 != None) {
            println("The download url is from github")
            val name = xml \ "result" \ "project" \ "name"
            val description = xml \ "result" \ "project" \ "description"
            //val downloadURL = xml \ "result" \ "project" \ "download_url"
            val tags = xml \ "result" \ "project" \ "tags"
            val mainLanguage = xml \ "result" \ "project" \ "analysis" \ "main_language_name"
            //println("Main proj language: " + mainLanguage)
            val tagArray = tags.text.split("\n")
            val tagsList = tagArray.toList
            /*val settings = Settings.settingsBuilder()
            .put("http.enabled", false)
            .put("path.home", "C:/Users/ohsal/Desktop/elasticsearch-2.3.0")  // Note the path I chose here
          val client = ElasticClient.local(settings.build)*/
            val settings = Settings.settingsBuilder()
              .put("path.home", "C:/Users/ohsal/Desktop/elasticsearch-2.3.0")
              .put("cluster.name", "elasticsearch-cluster").build()
            val client = ElasticClient.remote(settings, "104.154.100.222", 9300)
            Thread.sleep(2000)
            // now we can search for the document we indexed earlier

            val resp = client.execute {
              index into "projects" / "project" id i fields(
                "name" -> name.text,
                "description" -> description.text,
                "downloadURL" -> downloadURL.text,
                "tags" -> tagArray,
                "language" -> mainLanguage.text)
            }.await
            //println(resp)
          }
        }
        catch{
          case ex: SAXParseException => {
            println("SAXParseException")}
        }
      }
      val settings = Settings.settingsBuilder()
        .put("path.home", "C:/Users/ohsal/Desktop/elasticsearch-2.3.0")
        .put("cluster.name", "elasticsearch-cluster").build()
      val client = ElasticClient.remote(settings, "104.154.100.222", 9300)
      Thread.sleep(2000)
      val testResp = client.execute {
        search in "projects" / "project"
      }.await
      println(testResp)
  }
}

object Main extends App {
  val system = ActorSystem("HelloSystem")
  val xmlParse = system.actorOf(Props[XMLparser], name = "defaultArgument")
  val queryActor = system.actorOf(Props[reqQuery], "searchforStuff")
  val jsonActor = system.actorOf(Props[getJSON], "MattDamon")
  //xmlParse ! "defaultArgument"
  queryActor ! Params("name", "Turbinado", jsonActor)
  //queryActor ! Params("_id" , "27006")
}