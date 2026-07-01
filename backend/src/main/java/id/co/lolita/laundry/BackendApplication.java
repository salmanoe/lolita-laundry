package id.co.lolita.laundry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.TimeZone;

@SpringBootApplication
@EnableConfigurationProperties
public class BackendApplication {

    public static void main(String[] args) {
        // Pin the business timezone so LocalDate.now() (order dates, billing periods, reports,
        // invoice dates) AND the Postgres session CURRENT_DATE (pgjdbc derives the PG session
        // TimeZone from the JVM default) both resolve to the Indonesian calendar date — regardless
        // of the host TZ (dev Windows vs prod Ubuntu, Neon defaulting to UTC).
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Jakarta"));
        SpringApplication.run(BackendApplication.class, args);
    }
}
