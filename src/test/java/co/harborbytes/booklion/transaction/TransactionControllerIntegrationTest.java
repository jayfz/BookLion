package co.harborbytes.booklion.transaction;

import co.harborbytes.booklion.Application;
import co.harborbytes.booklion.ReplaceCamelCase;
import co.harborbytes.booklion.account.Account;
import co.harborbytes.booklion.account.AccountRepository;
import co.harborbytes.booklion.budget.BudgetRepository;
import co.harborbytes.booklion.user.Role;
import co.harborbytes.booklion.user.User;
import co.harborbytes.booklion.user.UserRepository;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.tools.Server;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = Application.class
)
@AutoConfigureMockMvc
@TestPropertySource(
        locations = "classpath:application-integrationtest.properties"
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayNameGeneration(ReplaceCamelCase.class)
@WithUserDetails("user@example.com")
public class TransactionControllerIntegrationTest {

    private final MockMvc mvc;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper jsonMapper;

    private Account testAccount;
    private Account testAccount2;
    private Account testAccount3;
    private User testUser;


    private int port;

    @BeforeAll
    public void createTestAccount() throws Exception {
        accountRepository.deleteAll();
        userRepository.deleteAll();
        testAccount = new Account();
        testAccount.setNumber("101");
        testAccount.setName("savings account");
        accountRepository.saveAndFlush(testAccount);

        testAccount2 = new Account();
        testAccount2.setNumber("501");
        testAccount2.setName("internet account expenses");

        accountRepository.saveAndFlush(testAccount2);


        testAccount3 = new Account();
        testAccount3.setNumber("502");
        testAccount3.setName("leasure account expenses");
        accountRepository.saveAndFlush(testAccount3);

        testUser = new User();
        testUser.setRole(Role.USER);
        testUser.setEmail("user@example.com");
        testUser.setPassword(passwordEncoder.encode("secret"));
        testUser.setFirstName("Giga");
        testUser.setLastName("Chad");
        userRepository.saveAndFlush(testUser);

        Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082")
                .start();
    }

    @Autowired
    public TransactionControllerIntegrationTest(MockMvc mvc, AccountRepository accountRepository, ObjectMapper jsonMapper, TransactionRepository transactionRepository, UserRepository userRepository, @LocalServerPort int port, PasswordEncoder passwordEncoder) {
        this.port = port;

        System.out.println("STARTING ON PORT " + port);

        this.mvc = mvc;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jsonMapper = jsonMapper;
        this.jsonMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
    }


    @BeforeEach
    public void clearDatabase() {
        this.transactionRepository.deleteAll();
    }

    @Test
    void contextLoads() {
        assertThat(accountRepository).isNotNull();
        assertThat(transactionRepository).isNotNull();
        assertThat(userRepository).isNotNull();
    }

    @Nested
    class PostRequestsFail {
        @Nested
        class WithBadRequestStatus {

            @ParameterizedTest(name = "description: \"{0}\", debitAmount1: \"{1}\", creditAmount1: \"{2}\", debitAmount2: \"{3}\", creditAmount2: \"{4}\",")
            @CsvSource(
                    value = {
                            "p, 86000.32, 86000.32, 86000.32, 86000.32, 3",
                            "paying internet bill, 86000.32, 86000.32, 86000.32, 86000.32, 2",
                            "paying internet bill, 0.00, 0.00, 0.00, 0.00, 2",
                            "paying internet bill, 86000.32, 86000.32, 0.00, 0.00, 2",
                            "paying internet bill, 0.00, 0.00, 86000.32, 86000.32, 2",
                            "paying internet bill, 86000.32, 0.00, 86000.32, 0.00, 1",
                            "paying internet bill, 0.00, 86000.32, 0.00, 86000.32, 1",
                            "paying internet bill, 86000.32, 0.00, 86000.32, 0.00, 1"
                    },
                    nullValues = "null"

            )

            public void whenTransactionPayloadIsNotValid(String description, String debitAmount1, String creditAmount1, String debitAmount2, String creditAmount2, int expectedErrorCount) throws Exception {
                Map<String, Object> transactionPayload = new HashMap<>();
                List<Map<String, Object>> linesPayload = new LinkedList<>();
                transactionPayload.put("description", description);
                transactionPayload.put("lines", linesPayload);

                Map<String, Object> line = new HashMap<>();
                line.put("debitAmount", debitAmount1);
                line.put("creditAmount", creditAmount1);
                line.put("accountId", testAccount.getId());
                linesPayload.add(line);

                line = new HashMap<>();
                line.put("debitAmount", debitAmount2);
                line.put("creditAmount", creditAmount2);
                line.put("accountId", testAccount2.getId());
                linesPayload.add(line);

                failedPostRequest(transactionPayload, expectedErrorCount, status().isBadRequest());
            }
        }
    }

    @Nested
    class PostRequestSucceed{
        @Nested
        class WithCreatedStatus{


            @ParameterizedTest(name = "description: \"{0}\", debitAmount1: \"{1}\", creditAmount1: \"{2}\", debitAmount2: \"{3}\", creditAmount2: \"{4}\",")
            @CsvSource(
                    value = {

                            "paying internet bill, 86000.32, 0.00, 0.00, 86000.32",
                            "paying internet bill, 0.00, 86000.32, 86000.32, 0.00",
                    },
                    nullValues = "null"

            )

            public void whenTransactionPayloadIsValid(String description, String debitAmount1, String creditAmount1, String debitAmount2, String creditAmount2) throws Exception {
                Map<String, Object> transactionPayload = new HashMap<>();
                List<Map<String, Object>> linesPayload = new LinkedList<>();
                transactionPayload.put("description", description);
                transactionPayload.put("lines", linesPayload);

                Map<String, Object> line = new HashMap<>();
                line.put("debitAmount", debitAmount1);
                line.put("creditAmount", creditAmount1);
                line.put("accountId", testAccount.getId());
                linesPayload.add(line);

                line = new HashMap<>();
                line.put("debitAmount", debitAmount2);
                line.put("creditAmount", creditAmount2);
                line.put("accountId", testAccount2.getId());
                linesPayload.add(line);

                MockHttpServletRequestBuilder requestBuilder = post("/transactions").contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(transactionPayload));

                mvc.perform(requestBuilder)
                        .andDo(print())
                        .andExpect(status().isCreated())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.status", is("success")));

            }
        }
    }

    @Nested
    class GetRequestsFail{
        @Nested
        class WithBadRequestStatus{

            @ParameterizedTest( name = "id = {0}")
            @ValueSource(strings = {
                    "dog",
                    "cat",
                    "penguin",
                    "house",
                    "building",
                    "xyz",
                    "b010",
                    "11111111111111111111111",
                    "0",
                    "-1"
            })
            public void whenTransactionIdFormatIsNotValid(String transactionId) throws Exception{
                Integer expectedErrorCount = 1;
                MultiValueMap<String, String> queryParams = null;
                failedGetRequest(new byte[0], expectedErrorCount, transactionId, queryParams, status().isBadRequest());
            }

        }

        @Nested
        class WithNotFoundStatus{

            @ParameterizedTest(name = "Id: \"{0}\"")
            @ValueSource(strings = {
                    "1",
                    "2",
                    "3",
                    "4",
                    "5",
                    "6",
            })
            public void whenTransactionDoesNotExist(String id) throws Exception {
                Integer expectedErrorCount = 1;
                failedGetRequest(new byte[0], expectedErrorCount, id, null, status().isNotFound());
            }

        }
    }


    @Nested
    class GetRequestsSucceed{
        @Nested
        class WithOkayStatus{

            @Test
            public void whenTransactionExists() throws Exception {
                Long createdTransactionId = createTestTransaction();
                MockHttpServletRequestBuilder requestBuilder = get("/transactions/{id}", createdTransactionId).contentType(MediaType.APPLICATION_JSON).content(new byte[0]);

                mvc.perform(requestBuilder)
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.status", is("success")));
            }

            @Test
            public void whenPaginationAndSortingIsRequested() throws Exception {

                Long firstId = createTestTransaction("vacation");
                Long secondId = createTestTransaction("paying for internet bill");

                MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
                queryParams.add("page", "0");
                queryParams.add("size", "1");
                queryParams.add("sort", "description,asc");

                MockHttpServletRequestBuilder requestBuilder = get("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .queryParams(queryParams);

                mvc.perform(requestBuilder)
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.status", is("success")))
                        .andExpect(jsonPath("$.data[0].id", is(secondId.intValue())))
                        .andExpect(jsonPath("$.data[0].description", is("paying for internet bill")))
                        .andExpect(jsonPath("$.page.totalElements", is(2)))
                        .andExpect(jsonPath("$.page.first", is(true)))
                        .andExpect(jsonPath("$.page.last", is(false)))
                        .andExpect(jsonPath("$.page.order", is("description: ASC")));
            }
        }

    }

    @Nested
    class DeleteRequestsFail{
        @Nested
        class WithBadRequestStatus {
            @ParameterizedTest( name = "id = {0}")
            @ValueSource(strings = {
                    "dog",
                    "cat",
                    "penguin",
                    "house",
                    "building",
                    "xyz",
                    "b010",
                    "11111111111111111111111",
                    "0",
                    "-1"
            })
            public void whenTransactionIdFormatIsNotValid(String transactionId) throws Exception{
                Integer expectedErrorCount = 1;
                MultiValueMap<String, String> queryParams = null;
                failedDeleteRequest(new byte[0], expectedErrorCount, transactionId, status().isBadRequest());
            }
        }
    }


    @Nested
    class DeleteRequestSucceed {
        @Nested
        class WithNoContentStatus {
            @ParameterizedTest(name = "Id: \"{0}\"")
            @ValueSource(strings = {
                    "1",
                    "2",
                    "3",
                    "4",
                    "5",
                    "6",
            })
            public void whenTransactionDoesNotExist(String id) throws Exception {

                MockHttpServletRequestBuilder requestBuilder = delete("/transactions/{id}", id).contentType(MediaType.APPLICATION_JSON).content(new byte[0]);

                mvc.perform(requestBuilder)
                        .andDo(print())
                        .andExpect(status().isNoContent())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.status", is("success")));

            }

            @Test
            public void whenTransactionExists() throws  Exception{

                Long createdTransactionId = createTestTransaction();
                MockHttpServletRequestBuilder requestBuilder = delete("/transactions/{id}", createdTransactionId).contentType(MediaType.APPLICATION_JSON).content(new byte[0]);

                mvc.perform(requestBuilder)
                        .andDo(print())
                        .andExpect(status().isNoContent())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.status", is("success")));
            }
        }
    }

    public void failedPostRequest(Object content, final Integer expectedErrorCount, final ResultMatcher expectedHttpStatus) throws Exception {

        MockHttpServletRequestBuilder requestBuilder = post("/transactions").contentType(MediaType.APPLICATION_JSON);
        doFailedRequest(content, requestBuilder, expectedErrorCount, expectedHttpStatus);
    }

    public void failedGetRequest(Object content, final Integer expectedErrorCount, String id, MultiValueMap<String, String> queryParams, final ResultMatcher expectedHttpStatus) throws Exception {

        if (queryParams == null)
            queryParams = new LinkedMultiValueMap<>();

        StringBuilder uriTemplate = new StringBuilder("/transactions");
        if (id != null)
            uriTemplate.append(String.format("/%s", id));

        MockHttpServletRequestBuilder requestBuilder = get(uriTemplate.toString()).contentType(MediaType.APPLICATION_JSON).queryParams(queryParams);
        doFailedRequest(content, requestBuilder, expectedErrorCount, expectedHttpStatus);
    }

    public void failedDeleteRequest(Object content, final Integer expectedErrorCount, String id, final ResultMatcher expectedHttpStatus) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = delete("/transactions/{id}", id ).contentType(MediaType.APPLICATION_JSON);
        doFailedRequest(content, requestBuilder, expectedErrorCount, expectedHttpStatus);
    }

    public void doFailedRequest(Object content, MockHttpServletRequestBuilder builder, Integer expectedErrorCount, ResultMatcher expectedHttpStatus) throws Exception {

        if (content instanceof byte[] bytes) {
            builder.content(bytes);
        } else {
            String accountJsonString = jsonMapper.writeValueAsString(content);
            builder.content(accountJsonString);
        }

        mvc.perform(builder)
                .andDo(print())
                .andExpect(expectedHttpStatus)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is("fail")))
                .andExpect(jsonPath("$.errors.length()", is(expectedErrorCount)));
    }

    public Long createTestTransaction(){
        return createTestTransaction("paying internet bill");
    }

    public Long createTestTransaction(String description){

        Transaction transaction = new Transaction();
        transaction.setDescription(description);
        transaction.setCreatedAt(Instant.now());

        List<TransactionLine> lines = new ArrayList<>();

        TransactionLine line = new TransactionLine();
        line.setCreditAmount(new BigDecimal("26000"));
        line.setDebitAmount((new BigDecimal("0")));
        line.setTransaction(transaction);
        line.setAccount(testAccount);
        lines.add(line);

        line = new TransactionLine();
        line.setCreditAmount(new BigDecimal("0"));
        line.setDebitAmount((new BigDecimal("16000")));
        line.setTransaction(transaction);
        line.setAccount(testAccount2);
        lines.add(line);

        line = new TransactionLine();
        line.setCreditAmount(new BigDecimal("0"));
        line.setDebitAmount((new BigDecimal("10000")));
        line.setTransaction(transaction);
        line.setAccount(testAccount3);
        lines.add(line);

        transaction.setLines(lines);
        transaction.setUser(testUser);
        transactionRepository.save(transaction);
        return transaction.getId();
    }
}


