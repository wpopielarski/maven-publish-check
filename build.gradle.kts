import java.net.HttpURLConnection
import java.net.URL
import java.io.IOException
import java.util.Base64
import javax.xml.xpath.XPathFactory
import org.xml.sax.InputSource

tasks {
    register("isPublished") {
        doFirst {
            publishing.repositories.forEach { repository ->
                val repositoryURL = (repository as MavenArtifactRepository).url
                val username = repository.credentials.getUsername()
                val password = repository.credentials.getPassword()
                val group = project.group.toString()
                val version = project.version.toString()
                val name = project.name.toString()
                val artifactURL = "$repositoryURL${group?.replace('.', '/')}/${name}/maven-metadata.xml"
                println("Searching for existing artifact $name in version $version")
                if (artifactExists(artifactURL, username!!, password!!, version)) {
                    throw RuntimeException("Artifact already exist - terminating publishing process.")
                }
                println("Artifact not found. Proceeding to publish.")
            }
        }
    }
}

tasks["publish"].dependsOn(tasks["isPublished"])

fun artifactExists(repositoryUrl: String, username: String, password: String, currentVersion: String): Boolean {
    if (currentVersion.endsWith("-SNAPSHOT")) return false
    try {
        val timeout = 10000
        val connection = URL(repositoryUrl).openConnection() as HttpURLConnection
        connection.setRequestProperty("Authorization", "Basic " + getBase64EncodedCredentials(username, password))
        connection.setConnectTimeout(timeout)
        connection.setReadTimeout(timeout)
        connection.setRequestMethod("GET")
        val responseCode = connection.getResponseCode()
        if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            throw RuntimeException("Unauthorized maven user. Please provide valid username and password.")
        }
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val content = connection.inputStream.bufferedReader().readText()
            val xpath = XPathFactory.newInstance().newXPath()
            val rawVersions = xpath.evaluate("//versions", InputSource(content.byteInputStream()))
            val versions = rawVersions.split("\\s+".toRegex())
              .filter { !it.trim().isEmpty() }
              .map { it.trim() }
            return versions.contains(currentVersion)
        }
        return false
    } catch (ignored: IOException) {
        println(ignored.message)
        return false
    }
}

fun getBase64EncodedCredentials(username: String, password: String): String {
    val s = "$username:$password"
    return Base64.getEncoder().encodeToString(s.toByteArray())
}
