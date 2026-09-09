package gr.codelearn.springbootai.training.foundation.beans;

/**
 * Only registered when {@code app.foundation.premium-support=true} — see {@link BeansConfig#premiumSupportChannel()}.
 */
class PremiumSupportChannel {

	String channel() {
		return "premium-support";
	}
}