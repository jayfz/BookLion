package co.harborbytes.booklion.account;

import co.harborbytes.booklion.Application;
import co.harborbytes.booklion.ReplaceCamelCase;
import co.harborbytes.booklion.user.Role;
import co.harborbytes.booklion.user.User;
import co.harborbytes.booklion.user.UserRepository;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Null;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


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
class AccountControllerIntegrationTest {

    private final MockMvc mvc;
    private final AccountRepository accountRepository;
    private final ObjectMapper jsonMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;



    @Autowired
    public AccountControllerIntegrationTest(MockMvc mvc, AccountRepository accountRepository, UserRepository userRepository, ObjectMapper jsonMapper, PasswordEncoder passwordEncoder) {
        this.mvc = mvc;
        this.accountRepository = accountRepository;
        this.jsonMapper = jsonMapper;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jsonMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);

    }

    @BeforeAll
    private void setUpUser(){
        User user = new User();
        user.setRole(Role.USER);
        user.setEmail("user@example.com");
        user.setPassword(passwordEncoder.encode("secret"));
        user.setFirstName("Giga");
        user.setLastName("Chad");
        userRepository.save(user);
    }

    @BeforeEach

    public void clearDatabase() {
        this.accountRepository.deleteAll();
    }

    @Test
    void contextLoads() {
        assertThat(accountRepository).isNotNull();
    }


    @Nested
    class RequestsWithInvalidJsonBodyFailWithBadStatus {

        @Test

        public void whenNothingIsSent() throws Exception {
            final Integer expectedErrorCount = 1;
            failedPostRequest(new byte[0], expectedErrorCount, status().isBadRequest());
        }

        @Test

        public void whenNullIsSent() throws Exception {
            final Integer expectedErrorCount = 1;
            failedPostRequest(null, expectedErrorCount, status().isBadRequest());
        }

        @Test

        public void whenAnIntegerIsSent() throws Exception {
            final Integer expectedErrorCount = 1;
            failedPostRequest(1, expectedErrorCount, status().isBadRequest());
        }

        @Test

        public void whenAStringIsSent() throws Exception {
            final Integer expectedErrorCount = 1;
            failedPostRequest("hello world", expectedErrorCount, status().isBadRequest());
        }

        @Test

        public void whenABooleanIsSent() throws Exception {
            final Integer expectedErrorCount = 1;
            failedPostRequest(true, expectedErrorCount, status().isBadRequest());
        }

        @Test

        public void whenAnEmptyArrayIsSent() throws Exception {
            final Integer expectedErrorCount = 1;
            failedPostRequest((new Object[0]), expectedErrorCount, status().isBadRequest());
        }
    }

    @Nested
    class PostRequestsFail {
        @Nested
        class WithBadRequestStatus {

            @ParameterizedTest(name = "account number: \"{0}\", account name: \"{1}\"")

            @CsvSource(
                    value = {
                            "null, null, 2",
                            "'','', 2",
                            "897, z, 2",
                            "42c, savings, 1",
                            "780, savings, 1",
                            "1001, savings, 1",
                            "101, a, 1",
                            "101, 5, 1",
                            "101, z, 1",

                    },
                    nullValues = "null"
            )
            public void whenAccountPayloadIsNotValid(String accountNumber, String accountName, int expectedErrorCount) throws Exception {
                Map<String, Object> payload = new HashMap<>();
                payload.put("number", accountNumber);
                payload.put("name", accountName);
                failedPostRequest(payload, expectedErrorCount, status().isBadRequest());
            }
        }

        @Nested
        class WithConflictStatus {
            @Test

            public void whenAccountNumberIsNotUnique() throws Exception {
                final String accountNumber = "101";
                final String accountName = "savings account";
                createTestAccount(accountNumber, accountName);

                final String anotherName = "checking account";
                final Integer expectedErrorCount = 1;

                Map<String, Object> payload = new HashMap<>();
                payload.put("number", accountNumber);
                payload.put("name", anotherName);

                failedPostRequest(payload, expectedErrorCount, status().isConflict());
            }


            @Test

            public void whenAccountNameIsNotUnique() throws Exception {
                final String accountNumber = "101";
                final String accountName = "savings account";
                createTestAccount(accountNumber, accountName);

                final String anotherNumber = "102";
                final Integer expectedErrorCount = 1;

                Map<String, Object> payload = new HashMap<>();
                payload.put("number", anotherNumber);
                payload.put("name", accountName);

                failedPostRequest(payload, expectedErrorCount, status().isConflict());
            }
        }
    }

    @Nested
    class PostRequestsSucceed {

        @Nested
        class WithCreatedStatus {
            @ParameterizedTest(name = "account number: \"{0}\", account name: \"{1}\"")

            @CsvSource(
                    value = {
                            "101, savings account",
                            "102, checking account",
                            "103, investment account",
                            "104, transmetro card",
                            "202, icetex loan",
                            "303, tech stack",
                            "402, venta mercadolibre",
                            "502, food carulla"
                    }
            )
            public void whenAccountPayloadIsValid(String accountNumber, String accountName) throws Exception {

                Map<String, Object> payload = new HashMap<>();
                payload.put("number", accountNumber);
                payload.put("name", accountName);

                MockHttpServletRequestBuilder requestBuilder = post("/accounts").contentType(MediaType.APPLICATION_JSON);
                String accountJsonString = jsonMapper.writeValueAsString(payload);
                requestBuilder.content(accountJsonString);

                mvc.perform(requestBuilder)
                        .andDo(print())
                        .andExpect(status().isCreated())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.status", is("success")))
                        .andExpect(jsonPath("$.data.id", is(notNullValue())));
            }
        }
    }

    @Nested
    class GetRequestsFail {

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
            public void whenAccountIdFormatIsNotValid(String id) throws Exception {
                Integer expectedErrorCount = 1;
                failedGetRequest(new byte[0], expectedErrorCount, id, null, status().isBadRequest());
            }

            @ParameterizedTest(name = "Account number: \"{0}\"")
            @NullAndEmptySource
            @ValueSource(strings = {
                    "abc",
                    "3a7",
                    "5b2",
                    "693",
                    "hello",
                    "4344",
                    "no",
            })


            public void whenAccountWithNumberFormatIsNotValid(String accountNumber) throws Exception {
                Integer expectedErrorCount = 1;
                MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
                queryParams.add("accountNumber", accountNumber);
                failedGetRequest(new byte[0], expectedErrorCount, null, queryParams, status().isBadRequest());
            }
        }

        @Nested
        class WithNotFoundStatus {


            @ParameterizedTest(name = "account id: \"{0}\"")
            @ValueSource(strings = {
                    "1",
                    "2",
                    "3",
                    "4",
                    "5",
                    "6",
            })

            public void whenAccountDoesNotExist(String id) throws Exception {
                Integer expectedErrorCount = 1;
                failedGetRequest(new byte[0], expectedErrorCount, id, null, status().isNotFound());
            }

            @ParameterizedTest(name = "Account number: \"{0}\"")
            @ValueSource(strings = {
                    "402",
                    "307",
                    "522",
                    "273",
                    "321",
                    "434",
                    "564",
            })

            public void whenAccountWithNumberDoesNotExist(String accountNumber) throws Exception {
                Integer expectedErrorCount = 1;
                MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
                queryParams.add("accountNumber", accountNumber);
                failedGetRequest(new byte[0], expectedErrorCount, null, queryParams, status().isNotFound());
            }

        }
    }

    @Nested
    class GetRequestsSucceed {

        @Nested
        class WithOkayStatus {

            @Test
            public void whenNotRequestingAnSpecificAccount() throws Exception {

                createTestAccount("101", "savings account");
                createTestAccount("102", "checking account");

                Integer createdAccountsCount = 2;
                MockHttpServletRequestBuilder requestBuilder = get("/accounts").contentType(MediaType.APPLICATION_JSON);
                mvc.perform(requestBuilder)
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.status", is("success")))
                        .andExpect(jsonPath("$.data.length()", is(createdAccountsCount)))
                        .andExpect(jsonPath("$.page", is(notNullValue())))
                        .andExpect(jsonPath("$.page.totalElements", is(createdAccountsCount)))
                        .andExpect(jsonPath("$.page.first", is(true)));
            }


            @Test
            public void whenAccountExists() throws Exception {

                Long createdAccountId = createTestAccount("101", "savings account");

                MockHttpServletRequestBuilder requestBuilder = get("/accounts/{id}", createdAccountId).contentType(MediaType.APPLICATION_JSON);
                mvc.perform(requestBuilder)
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.status", is("success")))
                        .andExpect(jsonPath("$.data.id", is(createdAccountId.intValue())))
                        .andExpect(jsonPath("$.data.number", is(notNullValue())))
                        .andExpect(jsonPath("$.data.name", is(notNullValue())));
            }


            @Test
            public void whenPaginationAndSortingIsRequested() throws Exception {

                Long firstAccountId = createTestAccount("101", "savings account");
                Long secondAccountId = createTestAccount("102", "checking account");

                MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
                queryParams.add("page", "0");
                queryParams.add("size", "1");
                queryParams.add("sort", "name,asc");

                MockHttpServletRequestBuilder requestBuilder = get("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .queryParams(queryParams);

                mvc.perform(requestBuilder)
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.status", is("success")))
                        .andExpect(jsonPath("$.data[0].id", is(secondAccountId.intValue())))
                        .andExpect(jsonPath("$.data[0].number", is("102")))
                        .andExpect(jsonPath("$.data[0].name", is("checking account")))
                        .andExpect(jsonPath("$.page.totalElements", is(2)))
                        .andExpect(jsonPath("$.page.first", is(true)))
                        .andExpect(jsonPath("$.page.last", is(false)))
                        .andExpect(jsonPath("$.page.order", is("name: ASC")));
            }
        }
    }

    @Nested
    class PutRequestsFail {

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
                    "0",
                    "-1"
            })
            public void whenAccountIdFormatIsNotValid(String id) throws Exception {
                Integer expectedErrorCount = 1;
                final String accountNumber = "101";
                final String accountName = "savings account";

                Map<String, Object> payload = new HashMap<>();
                payload.put("number", accountNumber);
                payload.put("name", accountName);

                failedPutRequest(payload, expectedErrorCount, id, status().isBadRequest());
            }



            @ParameterizedTest(name = "account number: \"{0}\", account name: \"{1}\"")
            @CsvSource(
                    value = {
                            "null, null, 2",
                            "'','', 2",
                            "897, z, 2",
                            "42c, savings, 1",
                            "780, savings, 1",
                            "1001, savings, 1",
                            "101, a, 1",
                            "101, 5, 1",
                            "101, z, 1",

                    },
                    nullValues = "null"
            )
            public void whenAccountPayloadIsNotValid(final String accountNumber, final String accountName, final int expectedErrorCount) throws Exception {
                String accountId = "100";

                Map<String, Object> payload = new HashMap<>();
                payload.put("number", accountNumber);
                payload.put("name", accountName);

                failedPutRequest(payload, expectedErrorCount, accountId, status().isBadRequest());
            }

        }

        @Nested
        class WithConflictStatus {


            @Test
            public void whenCreatingANewAccountAndAccountNumberIsNotUnique() throws Exception {
                final String accountNumber = "101";
                final String accountName = "savings account";
                createTestAccount(accountNumber, accountName);

                final String anotherName = "checking account";
                final Integer expectedErrorCount = 1;
                final String nonExistentAccountId = "100";

                Map<String, Object> payload = new HashMap<>();
                payload.put("number", accountNumber);
                payload.put("name", accountName);

                failedPutRequest(payload, expectedErrorCount, nonExistentAccountId, status().isConflict());
            }


            @Test
            public void whenCreatingANewAccountAndAccountNameIsNotUnique() throws Exception {
                final String accountNumber = "101";
                final String accountName = "savings account";
                createTestAccount(accountNumber, accountName);

                final String nonExistentAccountId = "100";
                final String anotherNumber = "102";
                final Integer expectedErrorCount = 1;

                Map<String, Object> payload = new HashMap<>();
                payload.put("number", anotherNumber);
                payload.put("name", accountName);

                failedPutRequest(payload, expectedErrorCount, nonExistentAccountId, status().isConflict());
            }


            @Test
            public void whenUpdatingAnExistingAccountAndAccountNameIsNotUnique() throws Exception {
                final String accountNumber = "101";
                final String accountName = "savings account";
                final Long firstAccountId = createTestAccount(accountNumber, accountName);

                final String anotherAccountNumber = "102";
                final String anotherAccountName = "checking account";
                createTestAccount(anotherAccountNumber, anotherAccountName);

                final Integer expectedErrorCount = 1;

                Map<String, Object> payload = new HashMap<>();
                payload.put("number", accountNumber);
                payload.put("name", anotherAccountName);

                failedPutRequest(payload, expectedErrorCount, firstAccountId.toString(), status().isConflict());
            }

        }
    }

    @Nested
    class PutRequestsSucceed {

        @Nested
        class WithCreatedStatus{

            @Test
            public void whenCreatingANewAccount() throws Exception {

                Map<String, Object> payload = new HashMap<>();
                payload.put("number", "101");
                payload.put("name", "savings account");

                String jsonString = jsonMapper.writeValueAsString(payload);
                Long nonExistentAccountId = 100L;

                MockHttpServletRequestBuilder requestBuilder = put("/accounts/{id}", nonExistentAccountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonString);

                mvc.perform(requestBuilder)
                        .andDo(print())
                        .andExpect(status().isCreated())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.status", is("success")))
                        .andExpect(jsonPath("$.data.id", is(notNullValue())))
                        .andExpect(jsonPath("$.data.number", is("101")))
                        .andExpect(jsonPath("$.data.name", is("savings account")));

            }
        }

        @Nested
        class withOkayStatus{

            @Test
            public void whenUpdatingExistingAccount() throws Exception {

                Long id = createTestAccount("101", "checking account");

                Map<String, Object> payload = new HashMap<>();
                payload.put("number", "101");
                payload.put("name", "savings account");
                String jsonString = jsonMapper.writeValueAsString(payload);

                MockHttpServletRequestBuilder requestBuilder = put("/accounts/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonString);

                mvc.perform(requestBuilder)
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.status", is("success")))
                        .andExpect(jsonPath("$.data.id", is(notNullValue())))
                        .andExpect(jsonPath("$.data.number", is("101")))
                        .andExpect(jsonPath("$.data.name", is("savings account")));

            }
        }
    }


    @Nested
    class PatchRequestsFail {

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
                    "0",
                    "-1"
            })
            public void whenAccountIdFormatIsNotValid(String id) throws Exception {
                Integer expectedErrorCount = 1;
                final String accountNumber = "101";
                final String accountName = "savings account";

                Map<String, Object> payload = new HashMap<>();
                payload.put("number", accountNumber);
                payload.put("name", accountName);

                failedPatchRequest(payload, expectedErrorCount, id, status().isBadRequest());
            }


            @Test
            public void whenAccountFieldsTypesAreNotValid() throws Exception {

                Long id = createTestAccount("101", "checking account");
                Map<String, Object> payload = new HashMap<>();
                payload.put("name", true);
                String jsonString = jsonMapper.writeValueAsString(payload);

                MockHttpServletRequestBuilder requestBuilder = patch("/accounts/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonString);

                mvc.perform(requestBuilder)
                        .andDo(print())
                        .andExpect(status().isBadRequest())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.status", is("fail")));
            }

        }

        @Nested
        class WithNotFoundStatus {

            @Test
            public void whenAccountDoesNotExist() throws Exception {
                String nonExistingId = "144";
                Integer expectedErrorCount = 1;
                Map<String, String> content = new HashMap<>();
                content.put("name", "checking account");
                failedPatchRequest(content, expectedErrorCount, nonExistingId.toString(), status().isNotFound());
            }
        }

        @Nested
        class WithConflictStatus {

            @Test
            public void whenAccountNameIsNotUnique() throws Exception {

                Long firstAccount = createTestAccount("101", "savings account");
                createTestAccount("102", "checking account");

                Integer expectedErrorCount = 1;
                Map<String, String> content = new HashMap<>();
                content.put("name", "checking account");
                failedPatchRequest(content, expectedErrorCount, firstAccount.toString(), status().isConflict());
            }
        }
    }

    @Nested
    class PatchRequestsSucceed {

        @Nested
        class WithOkayStatus{

            @Test
            public void whenAccountToUpdateExistsAndNameIsUpdated() throws Exception {

                Long id = createTestAccount("101", "checking account");
                Map<String, Object> payload = new HashMap<>();
                payload.put("name", "savings account");
                String jsonString = jsonMapper.writeValueAsString(payload);

                MockHttpServletRequestBuilder requestBuilder = patch("/accounts/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonString);

                mvc.perform(requestBuilder)
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.status", is("success")))
                        .andExpect(jsonPath("$.data.id", is(notNullValue())))
                        .andExpect(jsonPath("$.data.number", is("101")))
                        .andExpect(jsonPath("$.data.name", is("savings account")));
            }
        }

    }

    @Transactional
    public Long createTestAccount(String number, String name) {
        Account account = new Account();
        account.setId(null);
        account.setNumber(number);
        account.setName(name);
        accountRepository.saveAndFlush(account);
        return account.getId();
    }


    public void failedPostRequest(Object content, Integer expectedErrorCount, ResultMatcher expectedHttpStatus) throws Exception {

        MockHttpServletRequestBuilder requestBuilder = post("/accounts").contentType(MediaType.APPLICATION_JSON);
        doFailedRequest(content, requestBuilder, expectedErrorCount, expectedHttpStatus);
    }

    public void failedGetRequest(Object content, final Integer expectedErrorCount, String id, MultiValueMap<String, String> queryParams, final ResultMatcher expectedHttpStatus) throws Exception {

        if (queryParams == null)
            queryParams = new LinkedMultiValueMap<>();

        StringBuilder uriTemplate = new StringBuilder("/accounts");
        if (id != null)
            uriTemplate.append(String.format("/%s", id));

        MockHttpServletRequestBuilder requestBuilder = get(uriTemplate.toString()).contentType(MediaType.APPLICATION_JSON).queryParams(queryParams);
        doFailedRequest(content, requestBuilder, expectedErrorCount, expectedHttpStatus);
    }

    public void failedPutRequest(Object content, Integer expectedErrorCount, String id, ResultMatcher expectedHttpStatus) throws Exception {

        MockHttpServletRequestBuilder requestBuilder = put("/accounts/{id}", id).contentType(MediaType.APPLICATION_JSON);
        doFailedRequest(content, requestBuilder, expectedErrorCount, expectedHttpStatus);
    }

    public void failedPatchRequest(Object content, final Integer expectedErrorCount, String id, final ResultMatcher expectedHttpStatus) throws Exception {

        MockHttpServletRequestBuilder requestBuilder = patch("/accounts/{id}", id).contentType(MediaType.APPLICATION_JSON);
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

}