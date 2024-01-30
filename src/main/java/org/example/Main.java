package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.vdurmont.emoji.EmojiParser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.DevTools;
//import org.openqa.selenium.devtools.v114.network.Network;
//import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.devtools.v121.network.Network;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.sql.Driver;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

enum XPathElements {
    USER("/html/body/div[5]/main/div[3]/div/div[1]/div/form/div[1]/input"),
    PASSWORD("/html/body/div[5]/main/div[3]/div/div[1]/div/form/div[2]/input"),
    TERMS_CONDITION("/html/body/div[5]/main/div[3]/div/div[1]/div/form/div[3]/label/div"),
    SIGN_IN("/html/body/div[5]/main/div[3]/div/div[1]/div/form/p[1]/input"),
    SECOND_PAGE_CONTINUE_BUTTON("/html/body/div[4]/main/div[2]/div[3]/div[1]/div/div/div[1]/div[2]/ul/li/a"),
    RESCHEDULE_APPOINTMENT_BOX("/html/body/div[4]/main/div[2]/div[2]/div/section/ul/li[4]/a/h5/span"),
    RESCHEDULE_APPOINTMENT("/html/body/div[4]/main/div[2]/div[2]/div/section/ul/li[4]/div/div/div[2]/p[2]/a"),

    LOCATION_LIST("/html/body/div[4]/main/div[4]/div/div/form/fieldset/ol/fieldset/div/div[2]/div[1]/div/li"),
    LOCATION_OTTAWA("/html/body/div[4]/main/div[4]/div/div/form/fieldset/ol/fieldset/div/div[2]/div[1]/div/li/select/option[5]"),
    LOCATION_VANCOUVER("/html/body/div[4]/main/div[4]/div/div/form/fieldset/ol/fieldset/div/div[2]/div[1]/div/li/select/option[8]"),
    LOCATION_HALIFAX("/html/body/div[4]/main/div[4]/div/div/form/fieldset/ol/fieldset/div/div[2]/div[1]/div/li/select/option[3]"),
    LOCATION_CALGARY("/html/body/div[4]/main/div[4]/div/div/form/fieldset/ol/fieldset/div/div[2]/div[1]/div/li/select/option[2]"),
    LOCATION_TORONTO("/html/body/div[4]/main/div[4]/div/div/form/fieldset/ol/fieldset/div/div[2]/div[1]/div/li/select/option[7]"),
    LOCATION_MONTREAL("/html/body/div[4]/main/div[4]/div/div/form/fieldset/ol/fieldset/div/div[2]/div[1]/div/li/select/option[4]"),
    LOCATION_QUEBEC_CITY("/html/body/div[4]/main/div[4]/div/div/form/fieldset/ol/fieldset/div/div[2]/div[1]/div/li/select/option[6]");
    private final String xPath;

    XPathElements(String xPath) {
        this.xPath = xPath;
    }

    public String getXPath() {
        return xPath;
    }
}

interface SeleniumService {
    List<Response> getAvailableDates();
}

interface DiscordClient {
    void sendMessage(String message);
}

@RestController
@AllArgsConstructor
@Slf4j
class Trigger {
    private final SeleniumService seleniumService;
    private final DiscordClient discordClient;

    @GetMapping(value = "/api/v1/trigger", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Response>> triggerAction() {
        return ResponseEntity.ok(seleniumService.getAvailableDates());
    }

    @GetMapping(value = "/api/v1/visitor/date/discord", produces = MediaType.APPLICATION_JSON_VALUE)
    public void message() {
        List<Response> responses = seleniumService.getAvailableDates();
        StringBuilder builder = new StringBuilder();
        for (Response response : responses) {
            builder.append(MarkDownParser.heading1(response.getCity()));
            builder.append(MarkDownParser.newLine());
            StringBuilder finalBuilder = builder;
            if (response.getDates().isEmpty()) {
                finalBuilder.append(MarkDownParser.bulletPoint(EmojiParser.parseToUnicode("No available dates :slight_frown: ")));
                finalBuilder.append(MarkDownParser.newLine());
            }
            response.getDates().forEach(date -> {
                finalBuilder.append(MarkDownParser.bulletPoint(date.getDate()));
                finalBuilder.append(MarkDownParser.newLine());
            });
            discordClient.sendMessage(builder.toString());
            builder = new StringBuilder();
        }
    }
}

@SpringBootApplication
@EnableAutoConfiguration
@ConfigurationPropertiesScan
@AllArgsConstructor
@EnableScheduling
public class Main {

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

}

@Service
@AllArgsConstructor
@Slf4j
@EnableScheduling
class SeleniumServiceImpl implements SeleniumService {

    private final UserConfig userConfig;

    public static String prettyDate(String oldDate) {
        LocalDate date = LocalDate.parse(oldDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyy"));
    }

    @Override
    public List<Response> getAvailableDates() {
        log.info("Started");
        ChromeDriver driver = new ChromeDriver();
        driver.get(userConfig.getUrl());
        inputElement(driver, XPathElements.USER.getXPath(), userConfig.getEmail());
        inputElement(driver, XPathElements.PASSWORD.getXPath(), userConfig.getPassword());
        clickElement(driver, XPathElements.TERMS_CONDITION.getXPath());
        clickElement(driver, XPathElements.SIGN_IN.getXPath());
        clickElement(driver, XPathElements.SECOND_PAGE_CONTINUE_BUTTON.getXPath());
        clickElement(driver, XPathElements.RESCHEDULE_APPOINTMENT_BOX.getXPath());

        //Network Ops
        DevTools tools = driver.getDevTools();
        tools.createSessionIfThereIsNotOne();
        tools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));
        AtomicReference<String> responseBody = new AtomicReference<>("");
        tools.addListener(Network.responseReceived(), responseReceived -> {
            String body = tools.send(Network.getResponseBody(responseReceived.getRequestId())).getBody();
            String url = responseReceived.getResponse().getUrl();
            if (body.contains("business_day") && url.contains("expedite")) {
                responseBody.set(body);
            }
        });
        clickElement(driver,  XPathElements.RESCHEDULE_APPOINTMENT.getXPath());
        List<Response> responseList = new ArrayList<>();
        for (Map.Entry<String, String> e : getLocations().entrySet()) {
            clickElement(driver, e.getValue());
            List<AppointmentDate> dates;
            List<AppointmentDate> formattedDates = new ArrayList<>();
            try {
                if (responseBody.get() != null && !responseBody.get().equals("")) {
                    dates = new ObjectMapper().readValue(responseBody.get(), TypeFactory.defaultInstance().constructCollectionType(List.class, AppointmentDate.class));
                    dates.forEach(date -> formattedDates.add(new AppointmentDate(prettyDate(date.getDate()), true)));
                }
            } catch (JsonProcessingException ex) {
                throw new RuntimeException(ex);
            }
            responseList.add(Response.builder().city(e.getKey()).dates(formattedDates).build());
            responseBody.set("");
        }
        driver.quit();
        return responseList;
    }

    private void inputElement(WebDriver driver, String xPath, String input) {
        WebElement element = driver.findElement(By.xpath(xPath));
        log.info("{} : {}", element.getTagName(), element.getText());
        element.sendKeys(input);
        try {
            Thread.sleep(userConfig.getIntervalInSeconds() * 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void clickElement(WebDriver driver, String xPath) {
        WebElement element = driver.findElement(By.xpath(xPath));
        log.info("{} : {}", element.getTagName(), element.getText());
        element.click();
        try {
            Thread.sleep(userConfig.getIntervalInSeconds() * 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> getLocations() {
        Map<String, String> map = new HashMap<>();
        map.put("Ottawa", XPathElements.LOCATION_OTTAWA.getXPath());
        map.put("Toronto", XPathElements.LOCATION_TORONTO.getXPath());
        map.put("Calgary", XPathElements.LOCATION_CALGARY.getXPath());
        map.put("Montreal", XPathElements.LOCATION_MONTREAL.getXPath());
        map.put("Halifax", XPathElements.LOCATION_HALIFAX.getXPath());
        map.put("Vancouver", XPathElements.LOCATION_VANCOUVER.getXPath());
        map.put("Quebec City", XPathElements.LOCATION_QUEBEC_CITY.getXPath());
        return map;
    }

}

@Configuration
@ConfigurationProperties(prefix = "selenium.us-visa.*")
@Data
class UserConfig {
    private String url;
    private String email;
    private String password;
    private long intervalInSeconds;
}

@Data
@Builder
class Response {
    private String city;
    private List<AppointmentDate> dates;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class AppointmentDate {
    private String date;
    private boolean business_day;
}

@Service
@Slf4j
class DiscordClientImpl implements DiscordClient {
    public void sendMessage(String message) {
        String requestBody = String.format("{\"content\" : \"%s\"}", message);
        log.info(requestBody);
        RestTemplate template = new RestTemplate();
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        try {
            Thread.sleep(2 * 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        template.postForEntity("http://itorion.dev.ca:8080/api/v1/discord/message", request, String.class);
    }
}