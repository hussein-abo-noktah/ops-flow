import {useLogin} from "@/modules/auth/hooks/useLogin.ts";
import {useForm} from "react-hook-form";
import {zodResolver} from "@hookform/resolvers/zod";
import {type LoginFormValues, loginSchema} from "@/modules/auth/schemas/loginSchema.ts";


export function LoginForm() {
    const loginMutation = useLogin();

    const {
        register,
        handleSubmit,
        formState: {
            errors,
            isSubmitting,
        },
    } = useForm<LoginFormValues>({
        resolver: zodResolver(loginSchema),
    });

    const  onSubmit = (values : LoginFormValues) => {
        loginMutation.mutate(values);
    }

    return (
        <form
            className="login-form"
            onSubmit={handleSubmit(onSubmit)}
        >
            <div className="login-field">
                <label htmlFor="email">
                    Email
                </label>

                <input
                    id="email"
                    type="email"
                    autoComplete="username"
                    placeholder="you@company.com"
                    aria-invalid={Boolean(errors.email)}
                    aria-describedby={errors.email ? 'email-error' : undefined}
                    {...register('email')}
                />

                {errors.email && (
                    <p id="email-error" className="login-error" role="alert">
                        {errors.email.message}
                    </p>
                )}
            </div>

            <div className="login-field">
                <label htmlFor="password">
                    Password
                </label>

                <input
                    id="password"
                    type="password"
                    autoComplete="current-password"
                    placeholder="Enter your password"
                    aria-invalid={Boolean(errors.password)}
                    aria-describedby={errors.password ? 'password-error' : undefined}
                    {...register('password')}
                />

                {errors.password && (
                    <p id="password-error" className="login-error" role="alert">
                        {errors.password.message}
                    </p>
                )}
            </div>

            {loginMutation.isError && (
                <p className="login-error login-error-banner" role="alert">
                    {loginMutation.error.message || 'Unable to sign in. Please try again.'}
                </p>
            )}

            <button
                type="submit"
                disabled={
                    isSubmitting ||
                    loginMutation.isPending
                }
            >
                {loginMutation.isPending
                    ? 'Signing in...'
                    : 'Sign in'}
            </button>
        </form>
    );
}
