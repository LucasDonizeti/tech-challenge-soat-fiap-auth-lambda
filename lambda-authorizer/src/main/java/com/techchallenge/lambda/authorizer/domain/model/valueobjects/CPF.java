package com.techchallenge.lambda.authorizer.domain.model.valueobjects;

import com.techchallenge.lambda.authorizer.domain.exceptions.ValidacaoValorException;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.InputMismatchException;

@Getter
@EqualsAndHashCode
public final class CPF {
    private final String valor;

    private CPF(String valor) {
        this.valor = validar(valor);
    }

    public static CPF of(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new ValidacaoValorException("CPF não pode ser nulo ou vazio");
        }
        return new CPF(valor);
    }

    private String validar(String cpf) {
        String cpfLimpo = cpf.replaceAll("\\D", "");

        if (!isValidCPF(cpfLimpo)) {
            throw new ValidacaoValorException("CPF inválido");
        }

        return cpfLimpo;
    }

    private boolean isValidCPF(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return false;
        }

        // Verifica se todos os dígitos são iguais
        if (cpf.matches("(\\d)\\1{10}")) {
            return false;
        }

        try {
            // Calcula primeiro dígito verificador
            int soma = 0;
            for (int i = 0; i < 9; i++) {
                soma += Character.getNumericValue(cpf.charAt(i)) * (10 - i);
            }
            int primeiroDigito = 11 - (soma % 11);
            if (primeiroDigito >= 10) {
                primeiroDigito = 0;
            }

            // Calcula segundo dígito verificador
            soma = 0;
            for (int i = 0; i < 10; i++) {
                soma += Character.getNumericValue(cpf.charAt(i)) * (11 - i);
            }
            int segundoDigito = 11 - (soma % 11);
            if (segundoDigito >= 10) {
                segundoDigito = 0;
            }

            // Verifica se os dígitos calculados correspondem aos dígitos do CPF
            return Character.getNumericValue(cpf.charAt(9)) == primeiroDigito &&
                    Character.getNumericValue(cpf.charAt(10)) == segundoDigito;
        } catch (InputMismatchException e) {
            return false;
        }
    }

    public String getFormatado() {
        if (valor == null || valor.length() != 11) {
            return valor;
        }
        return valor.substring(0, 3) + "." +
                valor.substring(3, 6) + "." +
                valor.substring(6, 9) + "-" +
                valor.substring(9);
    }

    @Override
    public String toString() {
        return getFormatado();
    }
}
