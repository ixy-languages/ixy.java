package de.tum.in.net.ixy.memory;

import de.tum.in.net.ixy.generic.IxyDmaMemory;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * A simple implementation of Ixy's direct memory addresses specification.
 * <p>
 * All the implementation of the class is automatically generated using Lombok.
 *
 * @author Esaú García Sánchez-Torija
 */
@ToString(onlyExplicitlyIncluded = true, doNotUseGetters = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, doNotUseGetters = true)
@RequiredArgsConstructor(
	staticName = "of",
	access = AccessLevel.PUBLIC,
	onConstructor_ = {@NotNull, @Contract(value = "_, _ -> new", pure = true)}
)
public final class DmaMemory implements IxyDmaMemory {

	@EqualsAndHashCode.Include
	@Getter(onMethod_ = {@Contract(pure = true)})
	@ToString.Include(name = "virtual", rank = 2)
	private final long virtualAddress;

	@EqualsAndHashCode.Include
	@Getter(onMethod_ = {@Contract(pure = true)})
	@ToString.Include(name = "physical", rank = 1)
	private final long physicalAddress;

	/**
	 * Creates a copy of another Ixy's direct memory address specification implementation.
	 *
	 * @param dmaMemory The direct memory address specification to copy.
	 * @return A copy of the {@code dmaMemory}.
	 */
	@Contract(value = "null -> fail; _ -> new", pure = true)
	public static @NotNull IxyDmaMemory of(@NotNull IxyDmaMemory dmaMemory) {
		if (!BuildConfig.OPTIMIZED && dmaMemory == null) throw new InvalidNullParameterException("dmaMemory");
		return of(dmaMemory.getVirtualAddress(), dmaMemory.getPhysicalAddress());
	}

	/**
	 * Creates a copy of another Ixy's direct memory address specification implementation.
	 *
	 * @param dmaMemory The direct memory address specification to copy.
	 * @return A copy of the {@code dmaMemory}.
	 */
	@SuppressWarnings("MethodParameterOfConcreteClass")
	@Contract(value = "null -> fail; _ -> new", pure = true)
	public static @NotNull IxyDmaMemory of(@NotNull DmaMemory dmaMemory) {
		if (!BuildConfig.OPTIMIZED && dmaMemory == null) throw new InvalidNullParameterException("dmaMemory");
		return of(dmaMemory.virtualAddress, dmaMemory.physicalAddress);
	}

}
