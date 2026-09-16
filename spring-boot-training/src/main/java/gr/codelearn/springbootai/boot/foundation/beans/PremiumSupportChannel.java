package gr.codelearn.springbootai.boot.foundation.beans;

/**
 * Only registered when {@code app.foundation.premium-support=true} — see {@link BeansConfig#premiumSupportChannel()}.
 */
class PremiumSupportChannel {

	String channel() {
		return "premium-support";
	}
}