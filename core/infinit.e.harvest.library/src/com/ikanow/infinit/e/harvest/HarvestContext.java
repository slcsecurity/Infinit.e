/*******************************************************************************
 * Copyright 2012, The Infinit.e Open Source Project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.ikanow.infinit.e.harvest;

import com.ikanow.infinit.e.data_model.utils.IkanowSecurityManager;
import com.ikanow.infinit.e.harvest.extraction.document.DuplicateManager;
import com.ikanow.infinit.e.harvest.extraction.document.HarvestStatus;

public interface HarvestContext {
	DuplicateManager getDuplicateManager();
	HarvestStatus getHarvestStatus();
	boolean isStandalone();
	int getStandaloneMaxDocs();
	IkanowSecurityManager getSecurityManager();
}
