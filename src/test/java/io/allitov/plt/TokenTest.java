package io.allitov.plt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class TokenTest {

    @Test
    void storesTokenParts() {
        Token token = new Token(TokenType.NUMBER, "42", 7);

        assertThat(token.type()).isEqualTo(TokenType.NUMBER);
        assertThat(token.value()).isEqualTo("42");
        assertThat(token.position()).isEqualTo(7);
    }

    @Test
    void rejectsInvalidTokenParts() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Token(null, "42", 0))
                .withMessage("Token type must not be null");
        assertThatIllegalArgumentException().isThrownBy(() -> new Token(TokenType.NUMBER, null, 0))
                .withMessage("Token value must not be empty");
        assertThatIllegalArgumentException().isThrownBy(() -> new Token(TokenType.NUMBER, "", 0))
                .withMessage("Token value must not be empty");
        assertThatIllegalArgumentException().isThrownBy(() -> new Token(TokenType.NUMBER, "42", -1))
                .withMessage("Token position must be non-negative");
    }
}
