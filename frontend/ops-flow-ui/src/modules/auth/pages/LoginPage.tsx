import {LoginForm} from "@/modules/auth/components/LoginForm.tsx";
import './LoginPage.css';


export function LoginPage() {

    return (
        <main className="login-page">
            <div className="login-brand" aria-label="OpsFlow">
                <span className="login-brand-mark" aria-hidden="true">O</span>
                OpsFlow
            </div>
            <section className="login-card" aria-labelledby="login-title">
                <header className="login-header">
                    <p className="login-eyebrow">YOUR WORK, IN FLOW</p>
                    <h1 id="login-title">Welcome back</h1>
                    <p>Sign in to your OpsFlow workspace.</p>
                </header>
                <LoginForm/>
            </section>
            <p className="login-footer">One place to keep operations moving.</p>
        </main>
    );
}
