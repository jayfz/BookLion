package co.harborbytes.booklion.budget;

import co.harborbytes.booklion.Application;
import co.harborbytes.booklion.ReplaceCamelCase;
import co.harborbytes.booklion.account.Account;
import co.harborbytes.booklion.account.AccountRepository;
import co.harborbytes.booklion.user.Role;
import co.harborbytes.booklion.user.User;
import co.harborbytes.booklion.user.UserRepository;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = Application.class
)
@AutoConfigureMockMvc
@TestPropertySource(
        locations = "classpath:application-integrationtest.properties"
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayNameGeneration(ReplaceCamelCase.class)
@WithUserDetails("user@example.com")
public class BudgetControllerIntegrationTest {

    private final MockMvc mvc;
    private final AccountRepository accountRepository;
    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper jsonMapper;

    private Account testAccount;
    private Account testAccount2;
    private User testUser;

    @BeforeAll
    public void createTestAccount() {
        accountRepository.deleteAll();
        budgetRepository.deleteAll();
        userRepository.deleteAll();

        testAccount = new Account();
        testAccount.setNumber("101");
        testAccount.setName("savings account");
        accountRepository.saveAndFlush(testAccount);

        testAccount2 = new Account();
        testAccount2.setNumber("102");
        testAccount2.setName("checking account");
        accountRepository.saveAndFlush(testAccount2);

        testUser = new User();
        testUser.setRole(Role.USER);
        testUser.setEmail("user@example.com");
        testUser.setPassword(passwordEncoder.encode("secret"));
        testUser.setFirstName("Giga");
        testUser.setLastName("Chad");
        userRepository.saveAndFlush(testUser);
    }

    @Autowired
    public BudgetControllerIntegrationTest(MockMvc mvc, AccountRepository accountRepository, ObjectMapper jsonMapper, BudgetRepository budgetRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {

        System.out.println("----constructor called -----");
        this.mvc = mvc;

        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.budgetRepository = budgetRepository;
        this.passwordEncoder = passwordEncoder;
        this.jsonMapper = jsonMapper;
        this.jsonMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
    }


    @BeforeEach
    public void clearDatabase() {
        this.budgetRepository.deleteAll();
    }

    @Test
    void contextLoads() {
        assertThat(accountRepository).isNotNull();
        assertThat(budgetRepository).isNotNull();
    }


    @Nested
    class PostRequestsFail {

        @Nested
        class WithBadRequestStatus {

            @ParameterizedTest(name = "account Id: \"{0}\"")
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
            public void whenAccountIdFormatIsNotValid(String id) throws Exception {
                Integer expectedErrorCount = 1;
                Map<String, Object> payload = new HashMap<>();
                payload.put("amount", "202300");
                payload.put("description", "expenses for the month");

                failedPostRequest(payload, expectedErrorCount, id, status().isBadRequest());
            }

            @ParameterizedTest(name = "amount: \"{0}\", description: \"{1}\"")
            @CsvSource(
                    value = {
                            "null, null, 1",
                            "'', '', 1",
                            "b4a, expenses, 1",
                            "293000, b, 1",
                    },
                    nullValues = "null"

            )

            public void whenBudgetPayloadIsNotValid(String amount, String description, Integer expectedErrorCount) throws Exception {
                Map<String, Object> payload = new HashMap<>();
                payload.put("amount", amount);
                payload.put("description", description);
                String existingAccountId = "1";
                failedPostRequest(payload, expectedErrorCount, existingAccountId, status().isBadRequest());
            }
        }

        @Nested
        class WithNotFoundStatus {

            @Test
            public void whenAccountDoesNotExist() throws Exception {
                BigDecimal amount = new BigDecimal("202400");
                String description = "max expenditure in furniture";

                Map<String, Object> payload = new HashMap<>();
                payload.put("amount", amount);
                payload.put("description", description);

                Integer expectedErrorCount = 1;
                String nonExistentAccountId = "100";
                failedPostRequest(payload, expectedErrorCount, nonExistentAccountId, status().isNotFound());
            }
        }
    }

    @Nested


    class PostRequestsSucceed {

        @Nested
        class WithCreatedStatus {

            @ParameterizedTest(name = "amount: {0}, description: {1}")
            @CsvSource(
                    value = {
                            "234400.22, tech expenses",
                            "34400.22, travel expenses",
                            "400.00, tigo expenses",
                            "623000.00, plane expenses",
                    }
            )
            public void whenBudgetPayloadIsValid(String amount, String description) throws Exception {
                String existingAccountId = "1";

                Map<String, Object> payload = new HashMap<>();
                payload.put("amount", amount);
                payload.put("description", description);

                MockHttpServletRequestBuilder requestBuilder = post("/accounts/{id}/budget", existingAccountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(payload));

                mvc.perform(requestBuilder)
                        .andDo(print())
                        .andExpect(status().isCreated())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.status", is("success")))
                        .andExpect(jsonPath("$.data.id", is(notNullValue())))
                        .andExpect(jsonPath("$.data.amount", is(amount)))
                        .andExpect(jsonPath("$.data.description", is(description)));
            }
        }
    }


    @Nested


    class GetRequestsFail {

        @Nested
        class WithBadRequestStatus {

            @ParameterizedTest(name = "Id: \"{0}\"")
            @ValueSource(strings = {
                    "dog",
                    "cat",
                    "penguin",
                    "house",
                    "building",
                    "xyz",
                    "b010",
                    "11111111111111111111111",
            })
            public void whenBudgetIdFormatIsNotValid(String id) throws Exception {
                Integer expectedErrorCount = 1;
                failedGetRequest(new byte[0], expectedErrorCount, id, null, status().isBadRequest());
            }
        }

        @Nested
        class WithNotFoundStatus {

            @ParameterizedTest(name = "Id: \"{0}\"")
            @ValueSource(strings = {
                    "1",
                    "2",
                    "3",
                    "4",
                    "5",
                    "6",
            })
            public void whenBudgetDoesNotExist(String id) throws Exception {
                Integer expectedErrorCount = 1;
                failedGetRequest(new byte[0], expectedErrorCount, id, null, status().isNotFound());
            }

        }
    }

    @Nested


    class GetRequestsSucceed {

        @Nested
        class WithOkayStatus {

            @Test
            public void whenNotRequestingAnSpecificBudget() throws Exception {

                createTestBudget("300400", "travel expenses", testAccount);
                createTestBudget("200600", "phone expenses", testAccount2);

                Integer createdBudgetsCount = 2;
                MockHttpServletRequestBuilder requestBuilder = get("/budgets").contentType(MediaType.APPLICATION_JSON);
                mvc.perform(requestBuilder)
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.status", is("success")))
                        .andExpect(jsonPath("$.data.length()", is(createdBudgetsCount)))
                        .andExpect(jsonPath("$.page", is(notNullValue())))
                        .andExpect(jsonPath("$.page.totalElements", is(createdBudgetsCount)))
                        .andExpect(jsonPath("$.page.first", is(true)));
            }

            @Test
            public void whenBudgetExists() throws Exception {

                Long budgetId = createTestBudget("300400", "travel expenses", testAccount);

                MockHttpServletRequestBuilder requestBuilder = get("/budgets/{id}", budgetId).contentType(MediaType.APPLICATION_JSON);
                mvc.perform(requestBuilder)
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.status", is("success")))
                        .andExpect(jsonPath("$.data.id", is(budgetId.intValue())))
                        .andExpect(jsonPath("$.data.amount", is("300400.00")))
                        .andExpect(jsonPath("$.data.description", is("travel expenses")))
                        .andExpect(jsonPath("$.data.account", is(notNullValue())));
            }

            @Test
            public void whenPaginationAndSortingIsRequested() throws Exception {

                Long firstId = createTestBudget("36000", "phone expenses", testAccount);
                Long secondId = createTestBudget("26000", "transmetro card", testAccount2);

                MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
                queryParams.add("page", "0");
                queryParams.add("size", "1");
                queryParams.add("sort", "amount,asc");

                MockHttpServletRequestBuilder requestBuilder = get("/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .queryParams(queryParams);

                mvc.perform(requestBuilder)
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.status", is("success")))
                        .andExpect(jsonPath("$.data[0].id", is(secondId.intValue())))
                        .andExpect(jsonPath("$.data[0].amount", is("26000.00")))
                        .andExpect(jsonPath("$.data[0].description", is("transmetro card")))
                        .andExpect(jsonPath("$.page.totalElements", is(2)))
                        .andExpect(jsonPath("$.page.first", is(true)))
                        .andExpect(jsonPath("$.page.last", is(false)))
                        .andExpect(jsonPath("$.page.order", is("amount: ASC")));
            }
        }

    }


    @Nested

    class PutRequestsFail {

        @Nested
        class WithBadRequestStatus {

            @ParameterizedTest(name = "account id: \"{0}\"")
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
            public void whenAccountIdFormatIsNotValid(String invalidAccountId) throws Exception {
                Integer expectedErrorCount = 1;
                Long existingAccountId = createTestBudget("300", "expenses", testAccount);

                Map<String, Object> payload = new HashMap<>();
                payload.put("amount", "300");
                payload.put("description", "car expenses");

                failedPutRequest(payload, expectedErrorCount, invalidAccountId, existingAccountId.toString(), status().isBadRequest());
            }

            @ParameterizedTest(name = "budget Id: \"{0}\"")
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
            public void whenBudgetIdFormatIsNotValid(String invalidBudgetId) throws Exception {
                Integer expectedErrorCount = 1;

                Map<String, Object> payload = new HashMap<>();
                payload.put("amount", "300");
                payload.put("description", "car expenses");

                failedPutRequest(payload, expectedErrorCount, testAccount.getId().toString(), invalidBudgetId, status().isBadRequest());
            }

            @ParameterizedTest(name = "amount: \"{0}\", description: \"{1}\", expectedErrorCount = {2}")
            @CsvSource(
                    value = {
                            "null, null, 1",
                            "'', '', 1",
                            "b4a, expenses, 1",
                            "293000, b, 1",
                            "b4a, a, 1",

                    },
                    nullValues = "null"

            )
            public void whenBudgetPayloadIsNotValid(String amount, String description, Integer expectedErrorCount) throws Exception {
                Long existingBudgetId = createTestBudget("200", "expenses", testAccount);
                Map<String, Object> payload = new HashMap<>();
                payload.put("amount", amount);
                payload.put("description", description);
                failedPutRequest(payload, expectedErrorCount, testAccount.getId().toString(), existingBudgetId.toString(), status().isBadRequest());
            }
        }

        @Nested
        class WithNotFoundStatus {
            @Test
            public void whenAccountDoesNotExists() throws Exception {
                Long budgetId = createTestBudget("2550", "travel and hotel expenses", testAccount);
                Integer expectedErrorCount = 1;
                String nonExistentAccountId = "100";

                Map<String, Object> payload = new HashMap<>();
                payload.put("amount", "2550");
                payload.put("description", "travel and hotel expenses");

                failedPutRequest(payload, expectedErrorCount, nonExistentAccountId, budgetId.toString(), status().isNotFound());
            }
        }

        @Nested
        class WithConflictStatus {
            @Test
            public void whenCreatingANewBudgetButAccountAlreadyHasABudget() throws Exception {
                createTestBudget("3260", "hotel and travel expenses", testAccount);
                Map<String, Object> payload = new HashMap<>();
                payload.put("amount", "6402");
                payload.put("description", "travel and hotel expenses");
                String nonExistingBudgetId = "100";
                Integer expectedErrorCount = 1;
                failedPutRequest(payload, expectedErrorCount, testAccount.getId().toString(), nonExistingBudgetId, status().isConflict());
            }
        }

    }

    @Nested


    class PutRequestsSucceed {

        @Nested
        class WithOkayStatus {

            @Test
            public void whenUpdatingExistingBudget() throws Exception {

                Long budgetId = createTestBudget("300400", "travel expenses", testAccount);

                Map<String, Object> payload = new HashMap<>();
                payload.put("amount", "300000.55");
                payload.put("description", "travel and hotel expenses");

                String jsonPayload = jsonMapper.writeValueAsString(payload);


                MockHttpServletRequestBuilder requestBuilder = put("/accounts/{accountId}/budget/{budgetId}", testAccount.getId(), budgetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload);

                mvc.perform(requestBuilder)
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.status", is("success")))
                        .andExpect(jsonPath("$.data.id", is(budgetId.intValue())))
                        .andExpect(jsonPath("$.data.amount", is("300000.55")))
                        .andExpect(jsonPath("$.data.description", is("travel and hotel expenses")))
                        .andExpect(jsonPath("$.data.account", is(notNullValue())));
            }
        }

        @Nested
        class WithCreatedStatus {

            @Test
            public void whenCreatingABudget() throws Exception {

                Map<String, Object> payload = new HashMap<>();
                payload.put("amount", "322000.00");
                payload.put("description", "travel and hotel expenses");

                String jsonPayload = jsonMapper.writeValueAsString(payload);
                String nonExistingBudgetId = "600";


                MockHttpServletRequestBuilder requestBuilder = put("/accounts/{accountId}/budget/{budgetId}", testAccount.getId(), nonExistingBudgetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload);

                mvc.perform(requestBuilder)
                        .andDo(print())
                        .andExpect(status().isCreated())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.status", is("success")))
                        .andExpect(jsonPath("$.data.id", is(notNullValue())))
                        .andExpect(jsonPath("$.data.amount", is("322000.00")))
                        .andExpect(jsonPath("$.data.description", is("travel and hotel expenses")))
                        .andExpect(jsonPath("$.data.account", is(notNullValue())));
            }
        }
    }


    @Nested

    class PatchRequestsFail {
        @Nested
        class WithBadRequestStatus {
            @ParameterizedTest(name = "budget Id: \"{0}\"")
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
            public void whenBudgetIdFormatIsNotValid(String invalidBudgetId) throws Exception {
                Integer expectedErrorCount = 1;

                Map<String, Object> payload = new HashMap<>();
                payload.put("amount", "300");
                payload.put("description", "car expenses");

                failedPatchRequest(payload, expectedErrorCount, invalidBudgetId, status().isBadRequest());
            }

            @ParameterizedTest(name = "amount: \"{0}\", description: \"{1}\"")
            @CsvSource(
                    value = {
                            "null, null, 2",
                            "abc, a, 2",
                            "'-1', '', 2",
                            "b4a, expenses, 1",
                            "293000, b, 1",
                    },
                    nullValues = "null"

            )
            public void whenBudgetPayloadIsNotValid(String amount, String description, Integer expectedErrorCount) throws Exception {
                Long existingBudgetId = createTestBudget("200", "expenses", testAccount);


                Map<String, Object> payload = new HashMap<>();
                payload.put("amount", amount);
                payload.put("description", description);
                failedPatchRequest(payload, expectedErrorCount, existingBudgetId.toString(), status().isBadRequest());
            }
        }

        @Nested
        class WithNotFoundStatus {
            @ParameterizedTest(name = "budget Id: \"{0}\"")
            @ValueSource(strings = {
                    "1",
                    "2",
                    "3",
                    "4",
                    "5"
            })
            public void whenBudgetDoesNotExist(String notFoundBudgetId) throws Exception {
                Integer expectedErrorCount = 1;
                Map<String, Object> payload = new HashMap<>();
                payload.put("amount", "300");
                payload.put("description", "car expenses");
                failedPatchRequest(payload, expectedErrorCount, notFoundBudgetId, status().isNotFound());
            }
        }
    }


    @Nested


    class PatchRequestsSucceed {

        @Nested
        class WithOkayStatus {

            @Test
            public void whenBudgetPayloadIsValid() throws Exception {

                Long createdId = createTestBudget("400000", "yearly expenses", testAccount);
                Map<String, Object> payload = new HashMap<>();
                payload.put("amount", "232400.00");
                payload.put("description", "monthly expenses");

                MockHttpServletRequestBuilder builder = patch("/budgets/{id}", createdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(payload));

                mvc.perform(builder)
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.id", is(notNullValue())))
                        .andExpect(jsonPath("$.data.amount", is("232400.00")))
                        .andExpect(jsonPath("$.data.description", is("monthly expenses")));
            }
        }
    }

    @Nested


    class DeleteRequestsFail {
        @Nested
        class WithBadRequestStatus {

            @ParameterizedTest(name = "budget Id: \"{0}\"")
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
            public void whenBudgetIdFormatIsNotValid(String invalidBudgetId) throws Exception {
                Integer expectedErrorCount = 1;

                Map<String, Object> payload = new HashMap<>();
                payload.put("amount", "300");
                payload.put("description", "car expenses");

                failedDeleteRequest(payload, expectedErrorCount, invalidBudgetId, status().isBadRequest());
            }

        }
    }

    @Nested

    class DeleteRequestsSucceed {
        @Nested
        class WithNoContentStatus {

            @ParameterizedTest(name = "budget Id: \"{0}\"")
            @ValueSource(strings = {
                    "1",
                    "2",
                    "3",
                    "4"
            })
            public void whenBudgetIdFormatIsValid(String validBudgetId) throws Exception {

                MockHttpServletRequestBuilder builder = delete("/budgets/{id}", validBudgetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new byte[0]);

                mvc.perform(builder)
                        .andDo(print())
                        .andExpect(status().isNoContent());
            }

            @ParameterizedTest(name = "amount: \"{0}\", description: {1}")
            @CsvSource(value = {
                    "3600.00, car expenses",
                    "2903.00, house expenses",
                    "53.00, gym expenses",
            })
            public void whenDeletingExistingBudget(String amount, String description) throws Exception {

                Long createdBudget = createTestBudget(amount, description, testAccount);

                MockHttpServletRequestBuilder builder = delete("/budgets/{id}", createdBudget)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new byte[0]);

                mvc.perform(builder)
                        .andDo(print())
                        .andExpect(status().isNoContent());
            }

        }
    }

    public void failedPostRequest(Object content, final Integer expectedErrorCount, String id, final ResultMatcher expectedHttpStatus) throws Exception {

        MockHttpServletRequestBuilder requestBuilder = post("/accounts/{id}/budget", id).contentType(MediaType.APPLICATION_JSON);
        doFailedRequest(content, requestBuilder, expectedErrorCount, expectedHttpStatus);
    }

    public void failedGetRequest(Object content, final Integer expectedErrorCount, String id, MultiValueMap<String, String> queryParams, final ResultMatcher expectedHttpStatus) throws Exception {

        if (queryParams == null)
            queryParams = new LinkedMultiValueMap<>();

        StringBuilder uriTemplate = new StringBuilder("/budgets");
        if (id != null)
            uriTemplate.append(String.format("/%s", id));

        MockHttpServletRequestBuilder requestBuilder = get(uriTemplate.toString()).contentType(MediaType.APPLICATION_JSON).queryParams(queryParams);
        doFailedRequest(content, requestBuilder, expectedErrorCount, expectedHttpStatus);
    }

    public void failedPutRequest(Object content, final Integer expectedErrorCount, String accountId, String budgetId, final ResultMatcher expectedHttpStatus) throws Exception {

        MockHttpServletRequestBuilder requestBuilder = put("/accounts/{id}/budget/{budgetId}", accountId, budgetId).contentType(MediaType.APPLICATION_JSON);
        doFailedRequest(content, requestBuilder, expectedErrorCount, expectedHttpStatus);
    }

    public void failedPatchRequest(Object content, final Integer expectedErrorCount, String budgetId, final ResultMatcher expectedHttpStatus) throws Exception {

        MockHttpServletRequestBuilder requestBuilder = patch("/budgets/{budgetId}", budgetId).contentType(MediaType.APPLICATION_JSON);
        doFailedRequest(content, requestBuilder, expectedErrorCount, expectedHttpStatus);
    }

    public void failedDeleteRequest(Object content, final Integer expectedErrorCount, String budgetId, final ResultMatcher expectedHttpStatus) throws Exception {

        MockHttpServletRequestBuilder requestBuilder = delete("/budgets/{budgetId}", budgetId).contentType(MediaType.APPLICATION_JSON);
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

    public Long createTestBudget(String amount, String description, Account account) {
        Budget budget = new Budget();
        budget.setAmount(new BigDecimal(amount));
        budget.setDescription(description);
        budget.setAccount(account);
        budget.setUser(testUser);
        budgetRepository.saveAndFlush(budget);
        return budget.getId();
    }
}
