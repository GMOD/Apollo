$.fn.dataTableExt.oApi.fnAddDataAndDisplay = function ( oSettings, aData )
{
	/* Add the data */
	var iAdded = this.oApi._fnAddData( oSettings, aData );
	var nAdded = oSettings.aoData[ iAdded ].nTr;
	 
	/* Need to re-filter and re-sort the table to get positioning correct, not perfect
	 * as this will actually redraw the table on screen, but the update should be so fast (and
	 * possibly not alter what is already on display) that the user will not notice
	 */
	this.oApi._fnReDraw( oSettings );
	 
	/* Find it's position in the table */
	var iPos = -1;
	for( var i=0, iLen=oSettings.aiDisplay.length ; i<iLen ; i++ )
	{
		if( oSettings.aoData[ oSettings.aiDisplay[i] ].nTr == nAdded )
		{
			iPos = i;
			break;
		}
	}
	 
	/* Get starting point, taking account of paging */
	if( iPos >= 0 )
	{
		oSettings._iDisplayStart = ( Math.floor(i / oSettings._iDisplayLength) ) * oSettings._iDisplayLength;
		this.oApi._fnCalculateEnd( oSettings );
	}
	 
	this.oApi._fnDraw( oSettings );
	return {
		"nTr": nAdded,
		"iPos": iAdded
	};
};