<p:declare-step version='1.0' name="main"
                xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:err="http://www.w3.org/ns/xproc-error">
<p:input port="parameters" kind="parameter"/>
<p:output port="result">
  <p:pipe step="xsl-fo" port="result"/>
</p:output>

<p:xsl-formatter name="xsl-fo"
		 href="file:///tmp/out.pdf" content-type="application/pdf">
  <p:input port="source">
    <p:document href="envelope.fo"/>
  </p:input>
</p:xsl-formatter>

</p:declare-step>


