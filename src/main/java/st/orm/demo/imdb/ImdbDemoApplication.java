package st.orm.demo.imdb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ImdbDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImdbDemoApplication.class, args);
    }
}
