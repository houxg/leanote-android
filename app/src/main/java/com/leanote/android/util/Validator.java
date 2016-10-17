package com.leanote.android.util;


import com.leanote.android.R;

public class Validator {

    public enum ErrorType {USERNAME, PASSWORD, SITE_URL, EMAIL, TITLE, UNDEFINED}
    
    public static ErrorType getErrorType(int messageId) {
        if (messageId == R.string.username_only_lowercase_letters_and_numbers ||
                messageId == R.string.username_required || messageId == R.string.username_not_allowed ||
                messageId == R.string.username_must_be_at_least_four_characters ||
                messageId == R.string.username_contains_invalid_characters ||
                messageId == R.string.username_must_include_letters || messageId == R.string.username_exists ||
                messageId == R.string.username_reserved_but_may_be_available ||
                messageId == R.string.username_invalid) {
            return ErrorType.USERNAME;
        } else if (messageId == R.string.password_invalid) {
            return ErrorType.PASSWORD;
        } else if (messageId == R.string.email_cant_be_used_to_signup || messageId == R.string.email_invalid ||
                messageId == R.string.email_not_allowed || messageId == R.string.email_exists ||
                messageId == R.string.email_reserved) {
            return ErrorType.EMAIL;
        } else if (messageId == R.string.blog_name_required || messageId == R.string.blog_name_not_allowed ||
                messageId == R.string.blog_name_must_be_at_least_four_characters ||
                messageId == R.string.blog_name_must_be_less_than_sixty_four_characters ||
                messageId == R.string.blog_name_contains_invalid_characters ||
                messageId == R.string.blog_name_cant_be_used ||
                messageId == R.string.blog_name_only_lowercase_letters_and_numbers ||
                messageId == R.string.blog_name_must_include_letters || messageId == R.string.blog_name_exists ||
                messageId == R.string.blog_name_reserved ||
                messageId == R.string.blog_name_reserved_but_may_be_available ||
                messageId == R.string.blog_name_invalid) {
            return ErrorType.SITE_URL;
        } else if (messageId == R.string.blog_title_invalid) {
            return ErrorType.TITLE;
        }
        return ErrorType.UNDEFINED;
    }
}
